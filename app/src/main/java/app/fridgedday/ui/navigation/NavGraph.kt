package app.fridgedday.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import app.fridgedday.ui.home.HomeScreen
import app.fridgedday.ui.addedit.AddEditScreen
import app.fridgedday.ui.detail.ItemDetailScreen
import app.fridgedday.ui.scan.OcrConfirmScreen
import app.fridgedday.ui.scan.ScanScreen
import app.fridgedday.ui.settings.SettingsScreen
import app.fridgedday.ui.statistics.RecordScreen
import java.time.LocalDate
import java.time.format.DateTimeParseException

object Destinations {
    const val HOME = "home"
    const val ADD = "add"

    /** 항목 상세. 홈 위젯·외부 딥링크가 들어오는 지점이다. */
    const val DETAIL = "item/{id}"

    const val EDIT = "edit/{id}"
    const val SETTINGS = "settings"

    /** 기록. 통계 화면을 대체한 top-level 목적지다. */
    const val RECORD = "record"

    /** 기존 항목 딥링크. 수정 화면이 아니라 상세 화면으로 연결한다. */
    const val ITEM_DEEP_LINK = "fridgedday://item/{id}"

    /** 카메라·갤러리 OCR만 담당하는 task 화면. 상단 탭이 아니다. */
    const val SCAN = "scan"

    /** 홈 위젯의 바로 스캔 동작이 여는 딥링크. 기존 스캔 화면 하나만 연다. */
    const val SCAN_DEEP_LINK = "fridgedday://scan"

    /** OCR이 찾은 날짜만 넘겨받는 확인 화면. 사진이나 신뢰도는 경로에 담지 않는다. */
    const val OCR_CONFIRM = "ocr-confirm/{date}"

    /** OCR 확인에서 확정한 날짜로 시작하는 등록 화면. */
    const val REGISTER = "register/{date}"

    const val ARG_DATE = "date"

    /**
     * 새로 등록한 항목 id를 Today에 알리는 back stack entry 키.
     *
     * 등록 화면이 저장 성공 시 이전 entry(Today)의 savedStateHandle에 남기고, Today는 그 행을 실제로
     * 보여준 뒤 소비한다. 항목 id는 저장소에 이미 있는 값이며 새 이벤트/스키마가 아니다.
     */
    const val SAVED_ITEM_ID_KEY = "savedItemId"

    /**
     * 방금 `소비 완료`한 항목 id를 Today에 알리는 back stack entry 키.
     *
     * 상세 화면이 소비 완료 직후 Today로 돌아가면서 남기고, Today가 즉시 "실행 취소"를 띄운 뒤
     * 소비한다. 되돌리기는 기존 항목을 다시 활성 상태로 저장할 뿐 새 이벤트/스키마가 아니다.
     */
    const val CONSUMED_ITEM_ID_KEY = "consumedItemId"

    fun editRoute(id: Long) = "edit/$id"

    fun detailRoute(id: Long) = "item/$id"

    fun ocrConfirmRoute(date: LocalDate) = "ocr-confirm/$date"

    fun registerRoute(date: LocalDate) = "register/$date"
}

/** 경로 인자로 받은 ISO-8601 날짜만 통과시킨다. 잘못된 값은 null로 낮춘다. */
internal fun parseIsoDate(raw: String?): LocalDate? = try {
    raw?.let(LocalDate::parse)
} catch (_: DateTimeParseException) {
    null
}


/**
 * 앱의 모든 route.
 *
 * D-031: D-029/D-030의 route transition 효과를 제거해 모든 route가 즉시 전환되도록 복원한다.
 * 목적지·인자·딥링크·back stack 규칙은 이 정책과 무관하다.
 */
@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Destinations.HOME,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        composable(Destinations.HOME) {
            HomeScreen(navController)
        }

        composable(
            route = Destinations.ADD
        ) {
            AddEditScreen(navController, itemId = null)
        }

        composable(
            route = Destinations.REGISTER,
            arguments = listOf(navArgument(Destinations.ARG_DATE) { type = NavType.StringType })
        ) { backStackEntry ->
            val confirmedDate = parseIsoDate(
                backStackEntry.arguments?.getString(Destinations.ARG_DATE)
            )
            if (confirmedDate == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                AddEditScreen(
                    navController,
                    itemId = null,
                    initialConfirmedDate = confirmedDate
                )
            }
        }

        composable(
            route = Destinations.DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
            deepLinks = listOf(navDeepLink { uriPattern = Destinations.ITEM_DEEP_LINK })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id")
            if (id == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                ItemDetailScreen(navController = navController, itemId = id)
            }
        }

        composable(
            route = Destinations.EDIT,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id")
            AddEditScreen(navController, itemId = id)
        }

        composable(
            route = Destinations.SCAN,
            deepLinks = listOf(navDeepLink { uriPattern = Destinations.SCAN_DEEP_LINK })
        ) {
            ScanScreen(navController)
        }

        composable(
            route = Destinations.OCR_CONFIRM,
            arguments = listOf(navArgument(Destinations.ARG_DATE) { type = NavType.StringType })
        ) { backStackEntry ->
            val detectedDate = parseIsoDate(
                backStackEntry.arguments?.getString(Destinations.ARG_DATE)
            )
            if (detectedDate == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                OcrConfirmScreen(
                    detectedDate = detectedDate,
                    onConfirm = { confirmedDate ->
                        // 스캔·확인 화면은 task 화면이므로 등록 진입 시 스택에서 걷어낸다.
                        navController.navigate(Destinations.registerRoute(confirmedDate)) {
                            popUpTo(Destinations.HOME)
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(Destinations.SETTINGS) {
            SettingsScreen(navController)
        }

        composable(Destinations.RECORD) {
            RecordScreen(navController)
        }
    }
}

package app.fridgedday.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.material3.ColorProviders
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import app.fridgedday.MainActivity
import app.fridgedday.R
import app.fridgedday.FridgeDDayLightColorScheme
import app.fridgedday.data.db.AppDatabase
import app.fridgedday.ui.navigation.Destinations
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * 홈 화면 위젯.
 *
 * 헤더는 앱 이름 하나만 두고, Today 화면과 같은 임박 판정을 통과한 항목만 최대 [WIDGET_ROW_LIMIT]개
 * 보여준다. 위젯 전체를 누르면 앱이 열리고, 헤더의 스캔은 기존 스캔 화면으로, 각 행은 항목 상세로
 * 이동한다. 임박도는 색이 아니라 D-Day·기한 지남 문구가 전달하므로 색 대비나 색각에 기대지 않는다.
 */
class ExpiryWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val items = AppDatabase.getDatabase(context).itemDao().observeAll().first()
        val rows = buildWidgetRows(items, LocalDate.now())

        provideContent {
            GlanceTheme(
                colors = ColorProviders(
                    light = FridgeDDayLightColorScheme,
                    dark = FridgeDDayLightColorScheme
                )
            ) {
                WidgetContent(context, rows)
            }
        }
    }

    @Composable
    private fun WidgetContent(context: Context, rows: List<WidgetRow>) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(12.dp)
                .clickable(actionStartActivity(openAppIntent(context))),
            verticalAlignment = Alignment.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = context.getString(R.string.widget_title),
                    style = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onSurface
                    ),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight()
                )

                Text(
                    text = context.getString(R.string.widget_scan),
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onPrimaryContainer
                    ),
                    maxLines = 1,
                    modifier = GlanceModifier
                        .clickable(actionStartActivity(scanIntent(context)))
                        .background(GlanceTheme.colors.primaryContainer)
                        .cornerRadius(8.dp)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = GlanceModifier.height(6.dp))

            if (rows.isEmpty()) {
                Text(
                    text = context.getString(R.string.widget_empty),
                    style = TextStyle(
                        fontSize = 13.sp,
                        color = GlanceTheme.colors.onSurfaceVariant
                    ),
                    maxLines = 1
                )
            } else {
                rows.forEachIndexed { index, row ->
                    if (index > 0) {
                        Spacer(modifier = GlanceModifier.height(6.dp))
                    }
                    ItemRow(context, row)
                }
            }
        }
    }

    /** 항목 한 줄. 행 전체가 항목 상세 딥링크로 이동한다. */
    @Composable
    private fun ItemRow(context: Context, row: WidgetRow) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .clickable(actionStartActivity(itemDetailIntent(context, row.id))),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = row.name,
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = GlanceTheme.colors.onSurface
                ),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight()
            )

            Spacer(modifier = GlanceModifier.width(8.dp))

            Text(
                text = row.dDayLabel,
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.onSurface
                ),
                maxLines = 1
            )
        }
    }

    /** 위젯 본문 탭. 앱을 그대로 연다. */
    private fun openAppIntent(context: Context): Intent =
        Intent(context, MainActivity::class.java)

    /** 헤더의 스캔. 기존 스캔 화면 딥링크를 그대로 쓴다. */
    private fun scanIntent(context: Context): Intent =
        Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Destinations.SCAN_DEEP_LINK.toUri()
        }

    /** 행 탭. 이미 지원되는 항목 상세 딥링크 패턴을 그대로 채워 쓴다. */
    private fun itemDetailIntent(context: Context, itemId: Long): Intent =
        Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Destinations.ITEM_DEEP_LINK.replace("{id}", itemId.toString()).toUri()
        }
}

class ExpiryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ExpiryWidget()
}

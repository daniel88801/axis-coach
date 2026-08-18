package app.axis.coach.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import app.axis.coach.ui.components.AxisBrand
import app.axis.coach.ui.components.AxisPrimaryButton
import app.axis.coach.ui.theme.AxisTypography
import app.axis.coach.ui.theme.Fog
import app.axis.coach.ui.theme.FogDim
import app.axis.coach.ui.theme.FogMute
import app.axis.coach.ui.theme.Ink
import app.axis.coach.ui.theme.Lime
import app.axis.coach.ui.theme.Line

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    onComplete: () -> Unit,
) {
    var page by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { _ ->
        onComplete()
        onFinished()
    }

    val pages = listOf(
        Triple("AXIS", "Живой тренер: читает скелет, считает повторы и говорит, когда ломается линия.", "Начать"),
        Triple("Поставь телефон", "На 2–3 метра. Боком для приседа и отжиманий. Всё тело в кадре.", "Понятно"),
        Triple("Камера и голос", "AXIS смотрит на устройстве. Ничего не уходит в облако. Разреши камеру.", "Разрешить камеру"),
    )
    val current = pages[page]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
    ) {
        AxisBrand(size = 88.dp)
        Spacer(Modifier.height(32.dp))
        Text(current.first, style = AxisTypography.displayMedium, color = Fog)
        Spacer(Modifier.height(16.dp))
        Text(current.second, style = AxisTypography.bodyLarge, color = FogDim)
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            pages.indices.forEach { index ->
                val color by animateColorAsState(
                    if (index == page) Lime else Line,
                    label = "dot",
                )
                Box(
                    modifier = Modifier
                        .size(if (index == page) 18.dp else 8.dp, 8.dp)
                        .clip(CircleShape)
                        .background(color),
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        AxisPrimaryButton(
            text = current.third,
            onClick = {
                if (page < pages.lastIndex) {
                    page += 1
                } else {
                    val needed = buildList {
                        add(Manifest.permission.CAMERA)
                        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
                    }.filter {
                        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
                    }
                    if (needed.isEmpty()) {
                        onComplete()
                        onFinished()
                    } else {
                        permissionLauncher.launch(needed.toTypedArray())
                    }
                }
            },
        )
        if (page == pages.lastIndex) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Можно смотреть приложение и без этого. Живой разбор техники нуждается в камере.",
                style = AxisTypography.bodyMedium,
                color = FogMute,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

package ru.application.homemedkit.ui.screens

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.camera.core.TorchState
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.camera.view.TransformExperimental
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import ru.application.homemedkit.R.drawable.vector_barcode
import ru.application.homemedkit.R.drawable.vector_camera
import ru.application.homemedkit.R.drawable.vector_check
import ru.application.homemedkit.R.drawable.vector_datamatrix
import ru.application.homemedkit.R.drawable.vector_flash
import ru.application.homemedkit.R.drawable.vector_wrong
import ru.application.homemedkit.R.string.manual_add
import ru.application.homemedkit.R.string.text_connection_error
import ru.application.homemedkit.R.string.text_error_not_medicine
import ru.application.homemedkit.R.string.text_exit
import ru.application.homemedkit.R.string.text_explain_camera
import ru.application.homemedkit.R.string.text_grant
import ru.application.homemedkit.R.string.text_no
import ru.application.homemedkit.R.string.text_pay_attention
import ru.application.homemedkit.R.string.text_permission_grant_full
import ru.application.homemedkit.R.string.text_request_camera
import ru.application.homemedkit.R.string.text_try_again
import ru.application.homemedkit.R.string.text_yes
import ru.application.homemedkit.helpers.BLANK
import ru.application.homemedkit.helpers.DataMatrixAnalyzer
import ru.application.homemedkit.helpers.permissions.PermissionState
import ru.application.homemedkit.helpers.permissions.rememberPermissionState
import ru.application.homemedkit.models.events.Response.Default
import ru.application.homemedkit.models.events.Response.Duplicate
import ru.application.homemedkit.models.events.Response.Error
import ru.application.homemedkit.models.events.Response.IncorrectCode
import ru.application.homemedkit.models.events.Response.Loading
import ru.application.homemedkit.models.events.Response.NoNetwork
import ru.application.homemedkit.models.events.Response.Success
import ru.application.homemedkit.models.viewModels.ScannerViewModel

@OptIn(TransformExperimental::class)
@Composable
fun ScannerScreen(navigateUp: () -> Unit, navigateToMedicine: (Long, String, Boolean) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val model = viewModel<ScannerViewModel>()
    val response by model.response.collectAsStateWithLifecycle()

    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
            imageAnalysisResolutionSelector = ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                .setResolutionStrategy(
                    ResolutionStrategy(
                        android.util.Size(1920, 1080),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                    )
                )
                .build()
        }
    }

    BackHandler(onBack = navigateUp)
    if (cameraPermission.isGranted) Box {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                PreviewView(it).apply {
                    this.controller = controller
                    controller.bindToLifecycle(lifecycleOwner)
                }
            }
        )
        Canvas(Modifier.fillMaxSize()) {
            val length = if (size.width > size.height) size.height * 0.5f else size.width * 0.7f

            val frame = Path().apply {
                addRoundRect(
                    RoundRect(
                        cornerRadius = CornerRadius(16.dp.toPx()),
                        rect = Rect(
                            offset = Offset(center.x - length / 2, center.y - length / 2),
                            size = Size(length, length)
                        )
                    )
                )
            }
            clipPath(frame, ClipOp.Difference) { drawRect(Color.Black.copy(0.55f)) }
            drawPath(frame, Color.White, style = Stroke(5f))
        }
        Row(Modifier.fillMaxWidth(), Arrangement.End, Alignment.CenterVertically) {
            IconButton(
                onClick = {
                    controller.cameraControl?.enableTorch(controller.cameraInfo?.torchState?.value != TorchState.ON)
                }
            ) { Icon(painterResource(vector_flash), null, Modifier, Color.White) }
        }
    }
    else if (cameraPermission.showRationale) PermissionDialog(cameraPermission, navigateUp)
    else FirstTimeScreen(navigateUp, cameraPermission::launchRequest)

    when (val data = response) {
        Default -> controller.setImageAnalysisAnalyzer(
            Dispatchers.Main.immediate.asExecutor(),
            DataMatrixAnalyzer { model.fetch(context.filesDir, it) }
        )

        Loading -> {
            controller.clearImageAnalysisAnalyzer()
            LoadingDialog()
        }
        is Duplicate -> navigateToMedicine(data.id, BLANK, true)
        is Success -> navigateToMedicine(data.id, BLANK, false)
        is NoNetwork -> {
            controller.clearImageAnalysisAnalyzer()
            AddMedicineDialog(model::setDefault) { navigateToMedicine(0L, data.cis, false) }
        }

        IncorrectCode -> {
            controller.clearImageAnalysisAnalyzer()
            Snackbar(text_error_not_medicine)
        }

        Error -> {
            controller.clearImageAnalysisAnalyzer()
            Snackbar(text_try_again)
        }
    }
}

@Composable
fun Snackbar(id: Int) = Dialog({}, DialogProperties(usePlatformDefaultWidth = false)) {
    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color.Transparent),
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(
                    MaterialTheme.colorScheme.errorContainer,
                    MaterialTheme.shapes.extraSmall
                )
        ) {
            Text(
                text = stringResource(id),
                modifier = Modifier.padding(start = 16.dp),
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun FirstTimeScreen(navigateUp: () -> Unit, onGivePermission: () -> Unit) =
    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Column(Modifier, Arrangement.spacedBy(8.dp), Alignment.CenterHorizontally) {
            Image(painterResource(vector_camera), null, Modifier.size(64.dp))
            Text(
                text = stringResource(text_pay_attention),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = stringResource(text_explain_camera),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceAround, Alignment.CenterVertically) {
            Column(Modifier, Arrangement.spacedBy(12.dp), Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(vector_barcode),
                    contentDescription = null,
                    modifier = Modifier.size(128.dp),
                    contentScale = ContentScale.Crop,
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                )
                Image(
                    painter = painterResource(vector_wrong),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    contentScale = ContentScale.Crop
                )
            }
            Column(Modifier, Arrangement.spacedBy(12.dp), Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(vector_datamatrix),
                    contentDescription = null,
                    modifier = Modifier.size(128.dp),
                    contentScale = ContentScale.Crop,
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                )
                Image(
                    painter = painterResource(vector_check),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            TextButton(navigateUp) { Text(stringResource(text_exit)) }
            Button(onGivePermission) { Text(stringResource(text_grant)) }
        }
    }

@Composable
private fun AddMedicineDialog(setDefault: () -> Unit, navigateWithCis: () -> Unit) = AlertDialog(
    onDismissRequest = setDefault,
    confirmButton = { TextButton(navigateWithCis) { Text(stringResource(text_yes)) } },
    dismissButton = { TextButton(setDefault) { Text(stringResource(text_no)) } },
    title = { Text(stringResource(text_connection_error)) },
    text = { Text(stringResource(manual_add)) },
    icon = { Icon(Icons.Outlined.Info, null) }
)

@Composable
fun LoadingDialog() = Dialog({ }) {
    Box(Modifier.fillMaxSize(), Alignment.Center)
    { CircularProgressIndicator() }
}

@Composable
fun PermissionDialog(permission: PermissionState, onDismiss: () -> Unit) = Dialog(onDismiss) {
    ElevatedCard {
        Text(
            text = stringResource(text_request_camera),
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyLarge
        )
        TextButton(permission::launchRequest, Modifier.fillMaxWidth()) {
            Text(stringResource(text_permission_grant_full))
        }
    }
}
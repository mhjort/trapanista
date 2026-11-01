package app.trapanista.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.trapanista.data.Scenario
import app.trapanista.llm.Exercise
import app.trapanista.llm.ExerciseRole
import java.io.File

// A fixed path, so the result still resolves if Android kills the app while the camera is open.
private fun cameraFile(context: Context) =
    File(context.cacheDir, "exercises/scan.jpg").apply { parentFile?.mkdirs() }

private fun cameraUri(context: Context): Uri =
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cameraFile(context))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(viewModel: ScanViewModel, onBack: () -> Unit, onStart: (Scenario) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        if (saved) viewModel.scan(cameraUri(context), cameraFile(context))
    }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.scan(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan an exercise") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
            )
        },
    ) { padding ->
        val contentModifier = Modifier.fillMaxSize().padding(padding)
        when (val current = state) {
            ScanUiState.Idle -> SourcePicker(
                modifier = contentModifier,
                onCamera = { takePhoto.launch(cameraUri(context)) },
                onGallery = {
                    pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
            )
            ScanUiState.Scanning -> Centered(contentModifier) {
                CircularProgressIndicator()
                Text("Reading the exercise...", style = MaterialTheme.typography.bodyMedium)
            }
            is ScanUiState.Failed -> Centered(contentModifier) {
                Text(current.message, textAlign = TextAlign.Center)
                TextButton(onClick = viewModel::reset) { Text("Try again") }
            }
            is ScanUiState.Choose -> ExerciseList(
                exercises = current.exercises,
                modifier = contentModifier,
                onChoose = { exercise, role -> onStart(viewModel.choose(exercise, role)) },
                onScanAnother = viewModel::reset,
            )
        }
    }
}

@Composable
private fun Centered(modifier: Modifier, content: @Composable () -> Unit) {
    Column(
        modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { content() }
}

@Composable
private fun SourcePicker(modifier: Modifier, onCamera: () -> Unit, onGallery: () -> Unit) {
    Centered(modifier) {
        Text(
            "Photograph a pair exercise from your book and practise it here. For the clearest result, fill the frame with the exercise, in good light.",
            textAlign = TextAlign.Center,
        )
        Button(onClick = onCamera, Modifier.fillMaxWidth()) {
            Icon(Icons.Default.PhotoCamera, null)
            Spacer(Modifier.width(8.dp))
            Text("Take photo")
        }
        OutlinedButton(onClick = onGallery, Modifier.fillMaxWidth()) {
            Icon(Icons.Default.PhotoLibrary, null)
            Spacer(Modifier.width(8.dp))
            Text("Choose from gallery")
        }
    }
}

@Composable
private fun ExerciseList(
    exercises: List<Exercise>,
    modifier: Modifier,
    onChoose: (Exercise, ExerciseRole) -> Unit,
    onScanAnother: () -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("Who do you want to be?", style = MaterialTheme.typography.titleMedium) }
        items(exercises) { exercise ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(exercise.emoji, fontSize = 32.sp)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(exercise.titleIt, style = MaterialTheme.typography.titleMedium)
                            Text(
                                exercise.title,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Text(exercise.situation, style = MaterialTheme.typography.bodySmall)
                    // Only labels here: the brief carries that role's card, which the other side shouldn't see.
                    exercise.roles.forEach { role ->
                        OutlinedButton(onClick = { onChoose(exercise, role) }, Modifier.fillMaxWidth()) {
                            Text(role.label)
                        }
                    }
                }
            }
        }
        item {
            TextButton(onClick = onScanAnother, Modifier.fillMaxWidth()) { Text("Scan something else") }
        }
    }
}

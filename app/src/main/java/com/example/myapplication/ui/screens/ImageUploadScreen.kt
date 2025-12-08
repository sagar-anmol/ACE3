package com.example.myapplication.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.myapplication.R

@Composable
fun ImageUploadScreen(
    diagramResId: Int,
    onImageSelected: (Uri?) -> Unit
) {
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedImageUri = uri
        onImageSelected(uri)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Show diagram to copy
        Image(
            painter = painterResource(id = diagramResId),
            contentDescription = "Diagram",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(16.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = { launcher.launch("image/*") },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("Upload your drawing")
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Show preview if image selected
        selectedImageUri?.let { uri ->
            Image(
                painter = rememberAsyncImagePainter(uri),
                contentDescription = "Preview",
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(250.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            TextButton(onClick = {
                selectedImageUri = null
                onImageSelected(null)
            }) {
                Text("Remove image")
            }
        }
    }
}

package com.example.myapplication.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter

@Composable
fun ImageUploadScreen(
    diagramResId: Int?,
    selectedImage: Uri?,
    onImageSelected: (Uri?) -> Unit
) {
    val launcher = rememberLauncherForActivityResult(GetContent()) { uri ->
        onImageSelected(uri)
    }

    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {

        if (diagramResId != null && diagramResId != 0) {
            Image(
                painter = painterResource(id = diagramResId),
                contentDescription = null,
                modifier = Modifier.size(260.dp),
                contentScale = ContentScale.Fit
            )
        }



        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { launcher.launch("image/*") },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
        ) {
            Text("Upload your drawing", color = Color.White)
        }

        Spacer(modifier = Modifier.height(15.dp))

        selectedImage?.let {
            Image(
                painter = rememberAsyncImagePainter(it),
                contentDescription = null,
                modifier = Modifier.size(200.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

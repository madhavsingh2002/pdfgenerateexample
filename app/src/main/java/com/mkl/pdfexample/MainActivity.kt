package com.mkl.pdfexample

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mkl.pdfexample.ui.theme.PdfexampleTheme
import java.io.File
import java.io.FileOutputStream
import com.github.barteksc.pdfviewer.PDFView
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PdfexampleTheme {
                val navController = rememberNavController()
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavHost(navController = navController, startDestination = "PdfGeneratorScreen") {
                        composable("PdfGeneratorScreen") {
                            PdfGeneratorScreen(
                                onPreviewPdf = { pdfFile ->
                                    navController.navigate("PreviewPdfScreen/${Uri.encode(pdfFile.path)}")
                                },
                                navController
                            )
                        }
                        composable("PreviewPdfScreen/{pdfPath}") { backStackEntry ->
                            val pdfPath = backStackEntry.arguments?.getString("pdfPath")
                            pdfPath?.let {
                                PreviewPdfScreen(File(Uri.decode(it))) // Decode the file path before using it
                            }
                        }
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfGeneratorScreen(onPreviewPdf: (File) -> Unit, navController: NavController) {
    var text by remember { mutableStateOf("") }
    var pdfFile by remember { mutableStateOf<File?>(null) }
    val context = LocalContext.current
    val shareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {}
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "PDF Generator",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Enter Text") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                pdfFile = generatePdf(text, context)
            },
            enabled = text.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Generate PDF")
        }
        Spacer(modifier = Modifier.height(16.dp))
        pdfFile?.let { file ->
            Button(
                onClick = {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider", file
                    )
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    shareLauncher.launch(Intent.createChooser(shareIntent, "Share PDF"))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Share PDF")
            }
            Spacer(modifier = Modifier.height(16.dp))
            pdfFile?.let { file ->
                Button(
                    onClick = {
                        pdfFile?.let { file ->
                            // Navigate with encoded file path
                            navController.navigate("PreviewPdfScreen/${Uri.encode(file.path)}")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Preview PDF")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
@Composable
fun PreviewPdfScreen(pdfFile: File) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "PDF Preview",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            factory = { context ->
                PDFView(context, null).apply {
                    fromFile(pdfFile).load()
                }
            }
        )
    }
}

fun generatePdf(content: String, context: Context): File {
    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(300, 600, 1).create()
    val page = pdfDocument.startPage(pageInfo)
    val canvas: Canvas = page.canvas
    val paint = Paint()
    paint.textSize = 12f
    paint.isAntiAlias = true
    var yPosition = 25f
    content.split("\n").forEach { line ->
        canvas.drawText(line, 10f, yPosition, paint)
        yPosition += 15f
    }
    pdfDocument.finishPage(page)
    val pdfDir = File(context.getExternalFilesDir(null), "GeneratedPDFs")
    if (!pdfDir.exists()) pdfDir.mkdirs()
    val pdfFile = File(pdfDir, "GeneratedReport.pdf")
    pdfDocument.writeTo(FileOutputStream(pdfFile))
    pdfDocument.close()
    return pdfFile
}
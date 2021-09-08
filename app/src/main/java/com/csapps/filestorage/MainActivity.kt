package com.csapps.filestorage

import android.Manifest
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity() {

    val path = "pdfFolder"
    var externalFile: File? =null
    val pdfFileUrl = "https://www.learningcontainer.com/wp-content/uploads/2019/09/sample-pdf-file.pdf"
    lateinit var fileName: String
    private var downloadID: Long = -1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        getFileName()
        registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

        downloadPDF.setOnClickListener(View.OnClickListener {
            if (checkPermission()) createDownloadFileRequest()
        })
    }

    private fun getFileName(){
        fileName = pdfFileUrl.substring(pdfFileUrl.lastIndexOf("/") + 1)
        externalFile = File(getExternalFilesDir(null), fileName)
        pdfFileName.text = fileName
    }

    fun checkPermission(): Boolean{
        return if (ContextCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED) {
            true
        } else{
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
            false
        }
    }

    fun createDownloadFileRequest(){

        /*using download manage to download the pdf file*/
        var request: DownloadManager.Request = DownloadManager.Request(Uri.parse(pdfFileUrl))
        request.setTitle(fileName)
        request.setMimeType("application/pdf")
        request.setAllowedOverMetered(true)
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setDestinationUri(Uri.fromFile(externalFile!!))

        var manager: DownloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        downloadID = manager.enqueue(request)
        downloadProgress.visibility = View.VISIBLE
    }

    fun createFolder(){

        var uri: Uri? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            uri = FileProvider.getUriForFile(this, "$packageName.provider", externalFile!!)
        else
            uri = Uri.fromFile(externalFile)

        openDownloadedFile(uri)
    }

    fun openDownloadedFile(uri: Uri) {
        var intent = Intent(Intent.ACTION_VIEW)
        intent.apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        if(intent.resolveActivity(packageManager)!=null)
            startActivity(intent)
        else
            Toast.makeText(this, "No app found to view PDF file", Toast.LENGTH_SHORT).show()
    }

    private val onDownloadComplete: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                //Fetching the download id received with the broadcast
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)

                //Checking if the received broadcast is for our enqueued download by matching download id
                if (downloadID == id) {
                    downloadProgress.visibility = View.GONE
                    Handler().postDelayed(Runnable { createFolder() }, 500)

                    downloadID = -1
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}
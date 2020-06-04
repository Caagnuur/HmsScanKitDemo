package com.huawei.hmsscankitdemo

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.huawei.hms.hmsscankit.ScanUtil
import com.huawei.hms.ml.scan.HmsScanAnalyzerOptions


class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun newViewBtnClick(view: View?) {
        ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            BITMAP
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (permissions ==null  || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            return
        }
        if (requestCode == BITMAP) {
            val pickIntent = Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )
            pickIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
            startActivityForResult(pickIntent, REQUEST_CODE_PHOTO)
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data == null) {
            return
        }
        if (requestCode == REQUEST_CODE_PHOTO) {
            val path = getImagePath(this@MainActivity, data)
            if (TextUtils.isEmpty(path)) {
                return
            }

            val bitmap = ScanUtil.compressBitmap(this@MainActivity, path)

            val result1 = ScanUtil.decodeWithBitmap(
                this@MainActivity,
                bitmap,
                HmsScanAnalyzerOptions.Creator().setHmsScanTypes(0).setPhotoMode(false).create()
            )

            if (result1 != null && result1.size > 0) {
                if (!TextUtils.isEmpty(result1[0].getOriginalValue())) {
                    Toast.makeText(this, result1[0].getOriginalValue(), Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    companion object {
        const val BITMAP = 0x22
        const val REQUEST_CODE_PHOTO = 0x33
        private const val TAG = "MainActivity"


        private fun getImagePath(context: Context, data: Intent): String? {
            var imagePath: String? = null
            val uri = data.data

            val currentapiVersion = Build.VERSION.SDK_INT
            if (currentapiVersion > Build.VERSION_CODES.KITKAT) {
                if (DocumentsContract.isDocumentUri(context, uri)) {
                    val docId = DocumentsContract.getDocumentId(uri)
                    if ("com.android.providers.media.documents" == uri!!.authority) {
                        val id = docId.split(":").toTypedArray()[1]
                        val selection = MediaStore.Images.Media._ID + "=" + id
                        imagePath = getImagePath(
                            context,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            selection
                        )
                    } else if ("com.android.providers.downloads.documents" == uri.authority) {
                        val contentUri = ContentUris.withAppendedId(
                            Uri.parse("content://downloads/public_downloads"),
                            docId.toLong()
                        )
                        imagePath = getImagePath(context, contentUri, null)
                    } else {
                        Log.i(
                            TAG,
                            "getImagePath  uri.getAuthority():" + uri.authority
                        )
                    }
                } else if ("content".equals(uri!!.scheme, ignoreCase = true)) {
                    imagePath = getImagePath(context, uri, null)
                } else {
                    Log.i(
                        TAG,
                        "getImagePath  uri.getScheme():" + uri.scheme
                    )
                }
            } else {
                imagePath = getImagePath(context, uri, null)
            }
            return imagePath
        }

        /**
         * get image path from system album by uri
         */
        private fun getImagePath(
            context: Context,
            uri: Uri?,
            selection: String?
        ): String? {
            var path: String? = null
            val cursor =
                context.contentResolver.query(uri!!, null, selection, null, null)
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA))
                }
                cursor.close()
            }
            return path
        }
    }
}
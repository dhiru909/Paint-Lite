package com.justdevelopers.drawingapp

import android.Manifest
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog

class demo : AppCompatActivity(),View.OnClickListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demo)
    }

    override fun onClick(v: View?) {

    }

//    ibGallery.setOnClickListener{
//        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.M &&
//            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
//            showRationaleDialog(
//                "App would like to Access Your Camera",
//                "App uses camera to increase users' productivity"
//            )
//        }else{
//            cameraAndLocationResultLauncher.launch(
//                arrayOf(
//                    Manifest.permission.CAMERA,
//                    Manifest.permission.ACCESS_FINE_LOCATION
//                )
//            )
//        }
//    }
    private fun showRationaleDialog(
        title:String,
        message:String
    ){
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancel"){dialog,_->
                dialog.dismiss()
            }
        builder.create().show()
    }

}
package com.justdevelopers.drawingapp

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.view.ViewAnimationUtils
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.defaults.colorpicker.ColorPickerPopup;
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(){
    private var drawingView:DrawingView?=null
    private var progressView: TextView? = null
    var customProgressDialog: Dialog? = null
    val openGalleryLauncher :ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result->
                if(result.resultCode == RESULT_OK && result.data!=null){
                    val imageBackground:ImageView=findViewById(R.id.iv_background)
                    imageBackground.setImageURI(result.data?.data)
                }

        }

    private val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()){
                permissions->
                permissions.entries.forEach {
                    val permissionName = it.key
                    val isGranted =it.value
                    if(isGranted){
//                        Toast.makeText(this,"Permission granted now you can read the storage",Toast.LENGTH_LONG).show()
                        val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        openGalleryLauncher.launch(pickIntent)
                    }else{
                        if(permissionName==Manifest.permission.READ_EXTERNAL_STORAGE){
                            Toast.makeText(this,"oops you just denied the permission",Toast.LENGTH_LONG).show()

                        }
                    }
                }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        drawingView = findViewById(R.id.drawingView)
        drawingView!!.setSizeForBrush(5.toFloat())
        findViewById<ImageView>(R.id.ibUndo).setOnClickListener{
            drawingView?.undoOperation()
        }
        findViewById<ImageView>(R.id.ibRedo).setOnClickListener{
            drawingView?.redoOperation()
        }
        val ibBrush:ImageButton = findViewById(R.id.ibBrush)
        ibBrush.setOnClickListener{
            showBrushSizeChooserDialog()
        }
        val ibSave:ImageButton = findViewById(R.id.ibSave)
        ibSave.setOnClickListener{
            if(isReadStorageAllowed()){
                showProgressDialog()
                lifecycleScope.launch {
                    val flDrawingView:FrameLayout = findViewById(R.id.fl_drawing_view_container)
                    saveBitmapFile(getBitmapFromView(flDrawingView))
                }
            }else{
                Toast.makeText(this@MainActivity,"Failed: Give permission for Storage",Toast.LENGTH_SHORT)
                    .show()
            }
        }

        val ibGallery:ImageButton = findViewById(R.id.ibGallery)
        ibGallery.setOnClickListener{
            requestStoragePermission()
        }

        val ibColor:ImageButton = findViewById(R.id.ibColor)
        ibColor.setOnClickListener{
            showColorChooserDialog()
        }
    }

    private fun requestStoragePermission() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(
                this,Manifest.permission.READ_EXTERNAL_STORAGE)
        ){
            showRationaleDialog("Drawing App","Drawing App "+"needs to access your external permission")
        }else{
            requestPermission.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE))
        }
    }

    private fun showColorChooserDialog() {
        ColorPickerPopup.Builder(this@MainActivity).initialColor(
            drawingView!!.color
        ) // set initial color
            // of the color
            // picker dialog
            .enableBrightness(
                true
            ) // enable color brightness
            // slider or not
            .enableAlpha(
                true
            ) // enable color alpha
            // changer on slider or
            // not
            .okTitle(
                "Choose"
            ) // this is top right
            // Choose button
            .cancelTitle(
                "Cancel"
            ) // this is top left
            // Cancel button which
            // closes the
            .showIndicator(
                true
            ) // this is the small box
            // which shows the chosen
            // color by user at the
            // bottom of the cancel
            // button
            .showValue(
                true
            ) // this is the value which
            // shows the selected
            // color hex code
            // the above all values can be made
            // false to disable them on the
            // color picker dialog.
            .build()
            .show(object:ColorPickerPopup.ColorPickerObserver(){
                override fun onColorPicked(color: Int) {
                    drawingView!!.setColor(color)
                }

            })
    }

    private fun showBrushSizeChooserDialog(){
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush size: ")
        val btnConfirm:Button=brushDialog.findViewById(R.id.btnConfirm)

        val seekbarProgress=brushDialog.findViewById<TextView>(R.id.progress)

        seekbarProgress.textSize =25.toFloat()
        val seekBar = brushDialog.findViewById<SeekBar>(R.id.seekbar)

        btnConfirm.setOnClickListener{
            drawingView!!.setSizeForBrush(seekBar.progress.toFloat())
            brushDialog.dismiss()
        }
        brushDialog.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            brushDialog.dismiss()
        }
        seekBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar,
                                           progress: Int, fromUser: Boolean) {
               seekbarProgress.text = progress.toString()
            }

            override fun onStartTrackingTouch(seek: SeekBar) {
                seekbarProgress.text =seek.progress.toString()
            }

            override fun onStopTrackingTouch(seek: SeekBar) {
                // write custom code for progress is stopped

            }
        })

        brushDialog.show()
    }
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
    private fun isReadStorageAllowed():Boolean {
        val result = ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun getBitmapFromView(view:View): Bitmap {
        var returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable =view.background
        if(bgDrawable != null){
            bgDrawable.draw(canvas)
        }else{
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)
        return returnedBitmap
    }

    private suspend fun saveBitmapFile(mBitmap: Bitmap?):String{
        var result = ""
        withContext(Dispatchers.IO) {
            if(mBitmap != null){
                try {
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG,90,bytes)
//                    val ff=File(
//                        externalCacheDir?.absoluteFile.toString()+File.separator+"DrawingApp_"+System.currentTimeMillis()/1000
//                    )  if we want to save the bitmap in data
//                    folder of Drawing App in android/data/DrawingApp/cache
                    val f = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() +
                        File.separator + "DrawingApp_" + SimpleDateFormat("yyyyMMdd_HHmmss").format(
                        Date()
                    )+".png")
                    val fo = FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()
                    result = f.absolutePath
                    runOnUiThread {
                        customProgressDialog?.dismiss()
                        if(result.isNotEmpty()) {
                            Toast.makeText(
                                this@MainActivity,
                                "File saved successfully: $result",
                                Toast.LENGTH_SHORT
                            )
                                .show()
                            shareImage(result)
                        }
                        else
                            Toast.makeText(this@MainActivity,"Something went wrong saving the file",Toast.LENGTH_SHORT)
                                .show()
                    }
                }
                catch(e:Exception){
                    result = ""
                    e.printStackTrace()
                }
            }
        }
        return result
    }
    private fun showProgressDialog(){
        customProgressDialog = Dialog(this@MainActivity)
        customProgressDialog?.setCancelable(true)
        customProgressDialog?.setContentView(R.layout.custom_progress_dialog)
        customProgressDialog?.show()
    }
    private fun cancelProgressDialog(){
        if(customProgressDialog != null){
            customProgressDialog?.dismiss()
            customProgressDialog=null
        }
    }
    private fun shareImage(result:String?){
        MediaScannerConnection.scanFile(this, arrayOf(result),null){
            path,uri->
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM,uri)
            shareIntent.type = "image/png"
            startActivity(Intent.createChooser(shareIntent,"share"))

        }
    }
}
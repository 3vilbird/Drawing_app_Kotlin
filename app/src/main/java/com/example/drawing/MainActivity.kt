package com.example.drawing

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception


class MainActivity : AppCompatActivity() {

    private var drawingView: DrawingView? = null;
    private var mImageButtonCurrentPaint: ImageButton? = null;
    var customProgressDialog:Dialog? = null

    val openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val imageBcg: ImageView = findViewById(R.id.iv_background)
                imageBcg.setImageURI(result.data?.data) // this will give actual data image data

            }
        }


    val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {

                permissions ->
            permissions.entries.forEach {
                val permissionName = it.key
                val isGranted = it.value
                if (isGranted) {
                    Toast.makeText(
                        this,
                        "Permission granted  now you can read the storage files",
                        Toast.LENGTH_SHORT
                    ).show()
                    // to select the image from the gallery
                    val pickIntent =
                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    openGalleryLauncher.launch(pickIntent)


                } else {

                    if (permissionName == Manifest.permission.READ_EXTERNAL_STORAGE) {
                        Toast.makeText(
                            this,
                            "Permission denied to access the external storage  ",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }


            }

        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawingView = findViewById(R.id.drawing_view)
        drawingView?.setSizeForBrush(20.toFloat())

        var linearLayoutpaintColors = findViewById<LinearLayout>(R.id.ll_paint_colors)
        mImageButtonCurrentPaint = linearLayoutpaintColors[1] as ImageButton // getting the color

        mImageButtonCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.pallet_selected)
        )


        var id_brush: ImageButton = findViewById(R.id.ib_brush)

        id_brush.setOnClickListener {
            showBrushSizeChooserDialog()
        }
        var id_undo: ImageButton = findViewById(R.id.id_undo)

        id_undo.setOnClickListener {//
            drawingView?.onClickUndo()
        }

        val idGallery: ImageButton = findViewById(R.id.ib_gallery)

        idGallery.setOnClickListener {
            requestStoragePermission();
        }

        val idOnSave: ImageButton = findViewById(R.id.ib_save)

        idOnSave.setOnClickListener {
            if(isReadStorageAllowed()){
                //
                showProgressDialog() // call
                lifecycleScope.launch{
                    val flDrawingView :FrameLayout = findViewById(R.id.fl_drawing_view_container)
                    saveBitmapFile(getBitmapFromView(flDrawingView))
                }
            }
        }


    }

    private fun showBrushSizeChooserDialog() {
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush size: ")
        val smallBtn: ImageButton = brushDialog.findViewById(R.id.ib_small_brush)
        smallBtn.setOnClickListener {
            drawingView?.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        }
        val mediumBtn: ImageButton = brushDialog.findViewById(R.id.ib_medium_brush)
        mediumBtn.setOnClickListener {
            drawingView?.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        }
        val largeBtn: ImageButton = brushDialog.findViewById(R.id.ib_large_brush)
        largeBtn.setOnClickListener {
            drawingView?.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        }

        brushDialog.show()
    }

    //setColor
    fun paintClicked(view: View) {
        if (view != mImageButtonCurrentPaint) {
            var imageBotton = view as ImageButton
            var colorTag = imageBotton.tag.toString()
            drawingView?.setColor(colorTag)

            // to set selected bnt
            imageBotton.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_selected)
            )
// to set unselected btns
            mImageButtonCurrentPaint?.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_normal)
            )

            mImageButtonCurrentPaint = view
        }
    }


    /**
     * Shows rationale dialog for displaying why the app needs permission
     * Only shown if the user has denied the permission request previously
     */
    private fun showRationaleDialog(
        title: String,
        message: String,
    ) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        builder.create().show()
    }

    private  fun isReadStorageAllowed():Boolean{
        var result  = ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }


    private fun requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        ) {
            showRationaleDialog("Drawing app", "Drawing app wants to Access your External storage")
        } else {
// requets for permission
            requestPermission.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,


                )
            )

        }
    }


    // we can not store the view but we can store a bit map so go for a bit map

    private fun getBitmapFromView(view: View): Bitmap {
        //
        val retunedBitmap = Bitmap.createBitmap(
            view.width,
            view.height,
            Bitmap.Config.ARGB_8888
        ) // last args in colors and opacity
        val canvas = Canvas(retunedBitmap)
        val bgDrawable = view.background

        if (bgDrawable != null) {
            bgDrawable.draw(canvas)
        } else {
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)
     return retunedBitmap
    }
    // co-routine to store the data on the bit map
    private suspend fun saveBitmapFile(mBitmap: Bitmap?):String{
        //
        var result =""
        withContext(Dispatchers.IO){
            //
            if(mBitmap != null){
                try {
                     val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG,90,bytes) // store imag as PNG
                     val f = File(externalCacheDir?.absoluteFile.toString()+File.separator+"DrawingApp_"+System.currentTimeMillis()/1000+".png") // laction
                     val fo = FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()
                     result = f.absolutePath

                    runOnUiThread {
                        cancelProgressDialog() // since it is a task on the ui thread
                         if (result != ""){
                             Toast.makeText(this@MainActivity, "File saved successfully : $result", Toast.LENGTH_SHORT).show()
                             ShareImage(result)
                         }
                        else{
                             Toast.makeText(this@MainActivity, "Something us wrong in saving the image", Toast.LENGTH_SHORT).show()
                        }

                    }
                }catch (e:Exception){
                    result = ""
                    e.printStackTrace()
                }

            }
        }
        return  result
    }


    /**
     * to display a progress bar
     */

    private fun showProgressDialog() {
        customProgressDialog = Dialog(this@MainActivity)

        /*Set the screen content from a layout resource.
        The resource will be inflated, adding all top-level views to the screen.*/
        customProgressDialog?.setContentView(R.layout.dialog_custom_progress)

        //Start the dialog and display it on screen.
        customProgressDialog?.show()
    }


    /**
     * This function is used to dismiss the progress dialog if it is visible to user.
     */
    private fun cancelProgressDialog() {
        if (customProgressDialog != null) {
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }
    }


    /**
     * Sharing the image in socila media or something
     */
private  fun ShareImage(result:String){
     MediaScannerConnection.scanFile(this, arrayOf(result),null){
         path,uri->
         val ShareIntent = Intent()
         ShareIntent.action = Intent.ACTION_SEND
         ShareIntent.putExtra(Intent.EXTRA_STREAM,uri)
         ShareIntent.type="image/png"
         startActivity(Intent.createChooser(ShareIntent,"share"))
     }
}



}
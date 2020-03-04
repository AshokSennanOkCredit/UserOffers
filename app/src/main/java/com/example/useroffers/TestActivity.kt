package com.example.useroffers

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.useroffers.fragment.CameraFragment
import com.example.useroffers.utils.Picture

class TestActivity : AppCompatActivity() {


    private val MY_PERMISSIONS_REQUEST_CAMERA = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)
        // 2

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.CAMERA),
                MY_PERMISSIONS_REQUEST_CAMERA)
        }else{
            supportFragmentManager
                // 3
                .beginTransaction()
                // 4
                .add(R.id.container, CameraFragment.getInstance(), "cameraFragment")
                // 5
                .commit()
        }

    }
    fun cameraCapturedImage(picture: Picture) {
        Toast.makeText(this,"${picture.path}",Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_CAMERA -> {
                val i = 0
                val len = permissions.size
                if (grantResults.size > 0 && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    supportFragmentManager
                        // 3
                        .beginTransaction()
                        // 4
                        .add(R.id.container, CameraFragment.getInstance(), "cameraFragment")
                        // 5
                        .commit()
                }
            }
        }
    }
}

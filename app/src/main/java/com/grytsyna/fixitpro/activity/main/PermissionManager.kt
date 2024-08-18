package com.grytsyna.fixitpro.activity.main

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.grytsyna.fixitpro.R
import com.grytsyna.fixitpro.receiver.MyApplication
import com.grytsyna.fixitpro.common.Constants.REQUEST_CODE_PERMISSIONS

class PermissionManager(private val activity: AppCompatActivity) {

    fun checkAndRequestPermissions(): Boolean {
        return if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECEIVE_SMS)
            != PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS),
                REQUEST_CODE_PERMISSIONS
            )
            false
        } else {
            true
        }
    }

    fun handlePermissionsResult(requestCode: Int, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                (activity.application as MyApplication).readSmsFromArchive()
            } else {
                Toast.makeText(activity, activity.getString(R.string.permissions_not_granted_text), Toast.LENGTH_SHORT).show()
            }
        }
    }
}

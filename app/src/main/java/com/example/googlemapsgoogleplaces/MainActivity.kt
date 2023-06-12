package com.example.googlemapsgoogleplaces

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability


class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    private val ERROR_DIALOG_REQUEST = 9001
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val btnTst = findViewById<Button>(R.id.btnHelloTst)
        if (isServiceOk()) {
            btnTst.setOnClickListener {
                val intent = Intent(this, MapActivity::class.java)
                startActivity(intent)
            }
        }
    }

    fun isServiceOk(): Boolean {
        Log.d(TAG, "is service ok: checking google service version")
        val available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)
        if (available == ConnectionResult.SUCCESS) {
            Log.d(TAG, "Google service is working")
            return true
        } else if (GoogleApiAvailability.getInstance().isUserResolvableError(available)) {
            Log.d(TAG, "an error occure but we can fix")
            val dialog = GoogleApiAvailability.getInstance()
                .getErrorDialog(this, available, ERROR_DIALOG_REQUEST)
            dialog?.show()
        } else {
            Toast.makeText(this, "We can't make map req", Toast.LENGTH_LONG).show()
        }
        return false
    }
}
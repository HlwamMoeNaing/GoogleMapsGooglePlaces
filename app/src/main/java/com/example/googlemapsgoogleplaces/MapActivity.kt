package com.example.googlemapsgoogleplaces

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.media.Image
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.places.Places
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task
import java.io.IOException

class MapActivity : AppCompatActivity(), OnMapReadyCallback,
    GoogleApiClient.OnConnectionFailedListener {
    private val TAG = "MapActivity"
    private val FINE_LOCATION = android.Manifest.permission.ACCESS_FINE_LOCATION
    private val COURSE_LOCATION = android.Manifest.permission.ACCESS_COARSE_LOCATION
    private val LOCATION_PERMISSION_RQ_CODE = 123
    private var mLocationPermissionGranted = false
    private var googleMap: GoogleMap? = null
    private val DEFAULT_ZOOM = 15F
    private var mFuseLocationProviderClient: FusedLocationProviderClient? = null
    lateinit var mSearchText: AutoCompleteTextView
    lateinit var mGps: ImageView
    lateinit var autocompleteAdapter: PlaceAutocompleteAdapter
    lateinit var mGogleApiClient: GoogleApiClient
    private val LAT_LON_BAOUNG = LatLngBounds(LatLng(-40.0,-168.0),LatLng(71.0,136.0))
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        mSearchText = findViewById(R.id.input_search)
        mGps = findViewById(R.id.ic_gps)
        getLocationPermission()
        init()
    }

    private fun init() {
        Log.d(TAG, "init : Initializing")


        mGogleApiClient = GoogleApiClient.Builder(this).addApi(Places.GEO_DATA_API)
            .addApi(Places.PLACE_DETECTION_API)
            .enableAutoManage(this, this)
            .build()

        autocompleteAdapter = PlaceAutocompleteAdapter(this,mGogleApiClient,LAT_LON_BAOUNG,null)
        mSearchText.setAdapter(autocompleteAdapter)

        mSearchText.setOnEditorActionListener(object : TextView.OnEditorActionListener {
            override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
                if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || actionId == EditorInfo.IME_ACTION_DONE
                    || event?.action == KeyEvent.ACTION_DOWN
                    || event?.action == KeyEvent.KEYCODE_ENTER
                ) {
                    //execute our method for searching
                    geoLocate()
                }
                return false
            }

        })
        mGps.setOnClickListener {
            getDeviceLocation()
        }
        hideSoftKeyboard()
    }

    private fun geoLocate() {
        Log.d(TAG, "Geo locating")
        val searchString = mSearchText.text.toString()
        val getCoder = Geocoder(this)
        var list: ArrayList<Address> = arrayListOf()
        try {

            list = getCoder.getFromLocationName(searchString, 1) as ArrayList<Address>

        } catch (e: IOException) {
            Log.e(TAG, "Geo locate exception" + e.message)
        }

        if (list.size > 0) {
            val address = list[0]
            Log.d(TAG, "Found a location $address")
            moveCamera(
                LatLng(address.latitude, address.longitude),
                DEFAULT_ZOOM,
                address.getAddressLine(0)
            )
        }
    }


    fun initMap() {
        Log.d(TAG, "initilization map")
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    fun getDeviceLocation() {
        Log.d(TAG, "Getting the current device location")
        mFuseLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        try {

            val taskLocation = mFuseLocationProviderClient!!.lastLocation as Task
            taskLocation.addOnCompleteListener {
                if (it.isSuccessful) {
                    Log.e(TAG, "Found Location")
                    val currentLocation = it.result
                    moveCamera(
                        LatLng(currentLocation.latitude, currentLocation.longitude),
                        DEFAULT_ZOOM,
                        "My Location"
                    )

                } else {
                    Toast.makeText(this, "Unable to get location", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception " + e.message)
        }
    }

    fun moveCamera(latLng: LatLng, zoom: Float, title: String) {
        Log.e(TAG, "Moving camera to lat " + latLng.latitude + "lng :" + latLng.longitude)
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom))

        if (!title.equals("My Location")) {
            val option = MarkerOptions().position(latLng).title(title)
            googleMap?.addMarker(option)
        }

        hideSoftKeyboard()

    }

    private fun getLocationPermission() {
        Log.d(TAG, "getting location permission")
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (ContextCompat.checkSelfPermission(
                this.applicationContext,
                FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    COURSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                mLocationPermissionGranted = true
                initMap()
            } else {
                ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_RQ_CODE)
            }
        } else {
            ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_RQ_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        Log.d(TAG, "onRequestPermission called")
        mLocationPermissionGranted = false

        when (requestCode) {
            LOCATION_PERMISSION_RQ_CODE -> {
                if (grantResults.isNotEmpty()) {
                    for (i in grantResults.indices) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            mLocationPermissionGranted = false
                            Log.d(TAG, "onRequestPermission failed")
                            return
                        }
                    }
                    Log.d(TAG, "onRequestPermission granted")
                    mLocationPermissionGranted = true
                    // init our map
                    initMap()
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

    }


    override fun onMapReady(p0: GoogleMap) {
        Toast.makeText(this, "Map is ready", Toast.LENGTH_LONG).show()
        Log.d(TAG, "Map is ready")
        googleMap = p0
        if (mLocationPermissionGranted) {
            getDeviceLocation()
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                return
            }
            googleMap!!.isMyLocationEnabled = true
            googleMap?.uiSettings?.isMyLocationButtonEnabled = false
            init()
        }
    }

    private fun hideSoftKeyboard() {
        this.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
    }

    override fun onConnectionFailed(p0: ConnectionResult) {

    }

}
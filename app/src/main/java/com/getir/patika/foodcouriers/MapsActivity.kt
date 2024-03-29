package com.getir.patika.foodcouriers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import java.io.IOException
import java.util.Locale


class MapsActivity : FragmentActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var currentLocation: Location

    private lateinit var searchView: SearchView
    private lateinit var autocompleteFragment: AutocompleteSupportFragment //autocomplete search bar
    private lateinit var placesClient: PlacesClient // for search results

    private lateinit var textAddress: TextView
    private var marker: Marker? = null // global marker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        Places.initialize(applicationContext,getString(R.string.map_api_key)) //for auto search results
        placesClient = Places.createClient(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this) //for current location

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment  //use map in the layout
        mapFragment.getMapAsync(this)

        searchView = findViewById(R.id.search_bar) //find searchView in the layout
        searchLocation() //activate searchView with Listener

        textAddress = findViewById(R.id.textView2) //find text in the layout

        checkLocationPermission() // Check location permission

        //Find autoCompleteFragment in the layout, set the Place Fields
        autocompleteFragment = (supportFragmentManager.findFragmentById(R.id.place_autocomplete_fragment) as AutocompleteSupportFragment?)!!
        autocompleteFragment.setPlaceFields(
            listOf(
                Place.Field.ID,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG
            )
        )
        autoCompleteSearch() //activate autocomplete with Listener

    }

    private fun checkLocationPermission() { //location permission function
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // If the user has given location permission, get the location.
            getDeviceLocation() // get the current location
        } else {
            //Show location permission request
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                Companion.LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }
    private fun getDeviceLocation() { // find and go to the current location
        try {
            val locationResult = fusedLocationClient.lastLocation // try to reach the location
            locationResult.addOnCompleteListener { task ->
                if (task.isSuccessful && task.result != null) { // if results found
                    currentLocation = task.result
                    val address = getDeviceLocationAddress(currentLocation) //getting open address for a text
                    textAddress.text = address

                    val latLng = LatLng(currentLocation.latitude, currentLocation.longitude)
                    setMarker(latLng,address,18f)

                } else {
                    // Error case, go to the default location
                    Log.e("MapsActivity", "Current location is null. Using defaults.")
                    Log.e("MapsActivity", "Exception: %s", task.exception)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(0.0, 0.0), 15f))
                }
            }
        } catch (e: SecurityException) {
            //no location permission
            Log.e("Exception: %s", e.message.toString())
        }
    }
    private fun getDeviceLocationAddress(currentLoc: Location): String { // function for location to open address string
        val geocoder = Geocoder(this@MapsActivity, Locale.getDefault())
        var addressText = ""
        try {
            val addresses: List<Address>? = geocoder.getFromLocation(currentLoc.latitude, currentLoc.longitude, 1)

            if (addresses != null && addresses.isNotEmpty()) {
                val address: Address = addresses[0]
                val stringBuilder = StringBuilder()

                // Merge address lines
                for (i in 0..address.maxAddressLineIndex) {
                    stringBuilder.append(address.getAddressLine(i)).append("\n")
                }
                addressText = stringBuilder.toString()
            } else {
                addressText = "No address found"
            }
        } catch (e: IOException) {
            e.printStackTrace()
            addressText = "Unable to get address for the location"
        }

        return addressText // return all string address
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            Companion.LOCATION_PERMISSION_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // User granted location permission, get location.
                    getDeviceLocation()
                } else {
                    // User did not grant location permission. Send message
                    Toast.makeText(this,"Access to the location was not allowed",Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
    }
    private fun searchLocation(){ // SearchView search event
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean { // when enter the search
                if (!query.isNullOrEmpty()) {
                    val location = query.toString()
                    val geocoder = Geocoder(this@MapsActivity)
                    try {
                        val addressList = geocoder.getFromLocationName(location, 1)
                        if (addressList != null && addressList.isNotEmpty()) {
                            val address = addressList[0]

                            var lastAddress: String = ""

                            for (i in 0 until address.maxAddressLineIndex + 1) { // Combine address lines
                                lastAddress += if (i == 0) address.getAddressLine(i) else "\n" + address.getAddressLine(i)
                            }

                            textAddress.text = lastAddress //view address in the text

                            val latLng = LatLng(address.latitude, address.longitude)
                            setMarker(latLng,location,12f)

                        } else {
                            Toast.makeText(this@MapsActivity, "Location not found", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean { //text listener
                return false
            }
        })
    }
    override fun onMapReady(googleMap: GoogleMap) { // when map api is ready
        mMap = googleMap

    }
    private fun autoCompleteSearch(){ // autocomplete search bar with places
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener { // add listener
            override fun onPlaceSelected(place: Place) { // when a result selected
                val address = place.address.toString()
                textAddress.text = address //view address in the text

                val latLng = place.latLng
                setMarker(latLng,address,17f)
            }
            override fun onError(status: Status) {
                Toast.makeText(applicationContext, status.toString(), Toast.LENGTH_SHORT).show()
            }
        })
    }
    private fun setMarker(latLng: LatLng,address: String, zoomF: Float){ // camera and marker operations on the map
        marker?.remove() //remove the old marker
        val markerOptions = MarkerOptions().position(latLng).title(address).icon(bitmapDescriptorFromVector(this, R.drawable.pin_radar))
        marker = mMap.addMarker(markerOptions) // Mark the selected location on the map
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomF)) // Focus camera on selected location
    }
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1 //permission state val
    }

    private fun bitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor? { //Bitmap Convert function
        return ContextCompat.getDrawable(context, vectorResId)?.run {
            setBounds(0, 0, intrinsicWidth, intrinsicHeight)
            val bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
            draw(Canvas(bitmap))
            BitmapDescriptorFactory.fromBitmap(bitmap)
        }
    }
}

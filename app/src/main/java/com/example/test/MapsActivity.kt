package com.example.test

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.test.databinding.ActivityMapsBinding
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitString
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.Marker
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var lastLocation: Location
    private lateinit var fusedLocationClient : FusedLocationProviderClient
    private val gson = Gson()
    //localforecast metapi
    private val path = "https://in2000-apiproxy.ifi.uio.no/weatherapi/locationforecast/2.0/compact?"
    //sunrise metapi
    private val path2 = "https://in2000-apiproxy.ifi.uio.no/weatherapi/sunrise/2.0/.json?lat=40.7127&lon=-74.0059&date=2022-03-01&offset=-05:00"
    //visible planets api https://github.com/csymlstd/visible-planets-api
    private val path3 = "https://visible-planets-api.herokuapp.com/v2?"
    // nilu api
    private val path4 = "https://api.nilu.no/aq/utd/59.89869/10.81495/3?method=within&components=no2"


    lateinit var latitude: String
    lateinit var longitude: String

    companion object{
       private const val LOCATION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.setOnMarkerClickListener(this)
        setUpMap()


    }

    private fun setUpMap() {
        if (ActivityCompat.checkSelfPermission(this,  Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_REQUEST_CODE)
            return
        }
        mMap.isMyLocationEnabled = true
        fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
            if(location != null){
                lastLocation = location
                val currentLatLong = LatLng(location.latitude, location.longitude)
                latitude = location.latitude.toString()
                longitude = location.longitude.toString()

                placeMarkerOnMap(currentLatLong)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLong, 12f))



            }

        }
    }

    private fun placeMarkerOnMap(currentLatLong: LatLng) {
        val markerOptions = MarkerOptions().position(currentLatLong)
        markerOptions.title("$currentLatLong")
        mMap.addMarker(markerOptions)
        CoroutineScope(Dispatchers.IO).launch {
            getData()
        }



    }

    override fun onMarkerClick(p0: Marker)= false


    suspend fun getData()  {

        try {

            // værdata
            val response = Fuel.get(path+"lat=${latitude}&lon=$longitude").awaitString()

            val result = gson.fromJson(response, Base::class.java).properties?.timeseries?.get(0)?.data?.instant


            //sunrise, henter moonrise
            val response2 = Fuel.get(path2).awaitString()

            val result2 = gson.fromJson(response2, Base2::class.java).location?.time?.get(0)?.moonrise?.time.toString()


            //planeter

            val response3 = Fuel.get(path3+"latitude=${latitude}&longitude=$longitude").awaitString()
            val result3 = gson.fromJson(response3, Base3::class.java).data

            // nilu luftkvalitet
            val response4 = Fuel.get(path4).awaitString()
            //val result4 = gson.fromJson(response4, Base4::class.java).


            //lager en liste med planeter
           var planetene = ""
            if (result3 != null) {
                for(data in result3){
                    planetene += data.name.toString() +", "
                }
            }



            // legger i viewet

            if (result != null) {
                binding.text.text = " Værvarsel neste time:\n Temperatur: " + result.details?.air_temperature.toString()+"\n Luft fuktighet: "+ result.details?.relative_humidity.toString()+
                        "\n Vind styrke: "+ result.details?.wind_speed.toString() + "\n Skydekke: "+ result.details?.cloud_area_fraction.toString()+
                        "\n Moonrise: "+ result2+
                        "\n Planetene: "+ planetene
            }







        } catch(exception: Exception) {
            println("A network request exception was thrown: ${exception.message}")

        }
    }
}




// result generated from /json

data class Base(val type: String?, val geometry: Geometry?, val properties: Properties?)

data class Data(val instant: Instant?, val next_12_hours: Next_12_hours?, val next_1_hours: Next_1_hours?, val next_6_hours: Next_6_hours?)

data class Details(val air_pressure_at_sea_level: Number?, val air_temperature: Number?, val cloud_area_fraction: Number?, val relative_humidity: Number?, val wind_from_direction: Number?, val wind_speed: Number?)

data class Geometry(val type: String?, val coordinates: List<Number>?)

data class Instant(val details: Details?)

data class Meta(val updated_at: String?, val units: Units?)

data class Next_12_hours(val summary: Summary?)

data class Next_1_hours(val summary: Summary?, val details: Details?)

data class Next_6_hours(val summary: Summary?, val details: Details?)

data class Properties(val meta: Meta?, val timeseries: List<Timeseries>?)

data class Summary(val symbol_code: String?)

data class Timeseries(val time: String?, val data: Data?)

data class Units(val air_pressure_at_sea_level: String?, val air_temperature: String?, val cloud_area_fraction: String?, val precipitation_amount: String?, val relative_humidity: String?, val wind_from_direction: String?, val wind_speed: String?)



// klasser fra sunrise api
data class Base2(val location: Location1?, val meta: Meta2?)

data class High_moon(val desc: String?, val elevation: String?, val time: String?)

data class Location1(val height: String?, val latitude: String?, val longitude: String?, val time: List<Time>?)

data class Low_moon(val desc: String?, val elevation: String?, val time: String?)

data class Meta2(val licenseurl: String?)

data class Moonphase(val desc: String?, val time: String?, val value: String?)

data class Moonposition(val azimuth: String?, val desc: String?, val elevation: String?, val phase: String?, val range: String?, val time: String?)

data class Moonrise(val desc: String?, val time: String?)

data class Moonset(val desc: String?, val time: String?)

data class Moonshadow(val azimuth: String?, val desc: String?, val elevation: String?, val time: String?)

data class Solarmidnight(val desc: String?, val elevation: String?, val time: String?)

data class Solarnoon(val desc: String?, val elevation: String?, val time: String?)

data class Sunrise(val desc: String?, val time: String?)

data class Sunset(val desc: String?, val time: String?)

data class Time(val date: String?, val high_moon: High_moon?, val low_moon: Low_moon?, val moonphase: Moonphase?, val moonposition: Moonposition?, val moonrise: Moonrise?, val moonset: Moonset?, val moonshadow: Moonshadow?, val solarmidnight: Solarmidnight?, val solarnoon: Solarnoon?, val sunrise: Sunrise?, val sunset: Sunset?)





// klasser fra planet api
data class Base3(val meta: Meta3?, val data: List<Data1>?, val links: Links?)

data class Data1(val name: String?, val aboveHorizon: Boolean?, val nakedEyeObject: Boolean?)

data class Links(val self: String?)

data class Meta3(val time: String?, val latitude: Number?, val longitude: Number?, val elevation: Any?)


// klasser fra nilu api
data class Base4(val id: Number?, val zone: String?, val municipality: String?, val area: String?, val station: String?, val eoi: String?, val type: String?, val component: String?, val fromTime: String?, val toTime: String?, val value: Number?, val unit: String?, val latitude: Number?, val longitude: Number?, val timestep: Number?, val index: Number?, val color: String?, val isValid: Boolean?, val isVisible: Boolean?)



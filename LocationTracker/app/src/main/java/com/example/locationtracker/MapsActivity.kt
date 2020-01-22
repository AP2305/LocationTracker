package com.example.locationtracker

import android.Manifest
import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.TypeEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.os.*
import android.telecom.Call
import android.transition.Transition
import android.transition.TransitionManager
import android.util.Property
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.*
import androidx.core.animation.addListener
import androidx.core.net.toUri
import androidx.core.os.postDelayed
import androidx.core.widget.NestedScrollView
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_maps.*
import kotlinx.android.synthetic.main.marker_infos.*
import kotlinx.android.synthetic.main.marker_infos.view.*
import kotlinx.android.synthetic.main.nearby_markers_list.*
import org.xml.sax.helpers.LocatorImpl
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.Url
import java.io.InputStream
import java.lang.StringBuilder
import java.net.URL
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.ln


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var locationManager: LocationManager
    private var locationListener: LocationListener ?= null
    lateinit var mService: GoogleApiService
    private lateinit var bottomSheetBehaviour:BottomSheetBehavior<NestedScrollView>
    private lateinit var bottomListBehaviour:BottomSheetBehavior<NestedScrollView>
    internal lateinit var currentPlace : ResultData
    var mintime: Long = 10
    private lateinit var menu: Menu
    var prevLocation : Marker?= null
    var trackUserListener : ValueEventListener ?= null
    var userlocation :Marker ?= null
    var pathListener : ValueEventListener?= null
    var polyLine : Polyline?=null
    var typePlace = ""
    var nearbyPlaces = HashMap<String,LatLng>()
    var nearbyIndexs = ArrayList<LatLng>()
    var locatoinLists = ArrayList<LatLng>()
    var locList = ArrayList<LatLng>()


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        if (menu != null) {
            this.menu = menu
        }
        val menuinflater = menuInflater
        menuinflater.inflate(R.menu.mapmenu, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {

        TransitionManager.beginDelayedTransition(constraintLayout)

        if (item!!.itemId == R.id.search) {

            if(userlocation != null){
                searchbox.visibility = View.VISIBLE
                searchbtn.visibility = View.VISIBLE
                restaurantBtn.visibility = View.VISIBLE
                searchbox.setText("")
                searchbox.requestFocus()
            }

        }
        if (item!!.itemId == R.id.drawPath) {

            if(item.isCheckable){
                if (item.isChecked){
                    getLocList(false)
                    item.isChecked = false
                }else{
                    getLocList(true)
                    item.isChecked = true
                }
            }

        }
        if (item!!.itemId == R.id.nearby) {
            if(userlocation!=null) {
                var url = getUrl(userlocation!!.position.latitude, userlocation!!.position.longitude,"","",1000)
                getNearbyPlaces(url)
            }
        }

        if (item!!.itemId == R.id.trackStart) {

            if(item.isCheckable) {
                if(item.isChecked){
                    noTracking()
                    item.isChecked = false
                }else {
                    var trackUser = menu.findItem(R.id.trackUser)
                    trackUser.isChecked = false
                    trackingStart()
                    item.isChecked = true
                }
            }
        }

        if(item!!.itemId == R.id.trackUser){
            if(item.isCheckable) {
                if(item.isChecked){
                    trackUserStart(false)
                    item.isChecked = false
                }else {
                    noTracking()
                    var trackStart :MenuItem = menu.findItem(R.id.trackStart)
                    trackStart.isChecked = false
                    trackUserStart(true)
                    item.isChecked = true
                }
            }
        }

//        if(item!!.itemId == R.id.playPath){
//            println("starting play")
//            getLocList(true)
////            animateMarkerArr(locList!!,0)
//        }

        return super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        TransitionManager.beginDelayedTransition(bottomsheetLayout)

        val intent = intent
        var flag = intent.getBooleanExtra("accuracy", true)

        if (!flag) {
            mintime = 90000
        }

        TransitionManager.beginDelayedTransition(constraintLayout)

        restaurantBtn.setOnClickListener(View.OnClickListener {

//            var userLocation = userLoc
            if(userlocation!=null) {
                var url = getUrl(
                    userlocation!!.position.latitude,
                    userlocation!!.position.longitude,
                    "food",
                    "restaurant",
                    2000
                )
                getNearbyPlaces(url)

                searchbox.visibility = View.GONE
                searchbtn.visibility = View.GONE
                restaurantBtn.visibility = View.GONE
            }else{
                Toast.makeText(applicationContext,"Can't find User Location",Toast.LENGTH_SHORT).show()
            }

        })

        searchbtn.setOnClickListener(View.OnClickListener {

            var userLocation:LatLng = userlocation!!.position
            if(userLocation==null) {
                var location = getUserLocation()

                if(location!= null) {
                    userLocation = LatLng(location.latitude,location.longitude)
                }
            }else{
                userLocation = LatLng(23.02,72.57)
            }
            if (!searchbox.equals("")) {

                var url = getUrl(userLocation!!.latitude,userLocation!!.longitude,"",searchbox.text.toString(),5000)
                getNearbyPlaces(url)
            } else {
                var url = getUrl(userLocation!!.latitude,userLocation!!.longitude,"","",5000)
                getNearbyPlaces(url)
            }

            searchbox.visibility = View.GONE
            searchbtn.visibility = View.GONE
            restaurantBtn.visibility = View.GONE

        })

        bottomSheetSetup()

    }


    override fun onMapReady(googleMap: GoogleMap) {

        mMap = googleMap
        mService = Common.googleApiService
        userlocation = mMap.addMarker(MarkerOptions().position(LatLng(23.02,72.57)).title("Default"))

        mMap.setOnInfoWindowLongClickListener { marker ->
            if(userlocation!=null) {
                if(bottomSheetBehaviour.state == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetBehaviour.state = BottomSheetBehavior.STATE_COLLAPSED
                }
                if(bottomListBehaviour.state == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomListBehaviour.state = BottomSheetBehavior.STATE_COLLAPSED
                }
                var listt = ArrayList<LatLng>()
                listt.add(userlocation!!.position)
                listt.add(marker.position)
                mMap.clear()
                setPolyLines(listt)
                mMap.addMarker(MarkerOptions().position(marker.position).title(marker.title))
                var markerr = mMap.addMarker(MarkerOptions().position(userlocation!!.position).title("StartLocation"))
                var animator = getAnimator(markerr,marker.position)
                animator.start()
            }
        }


        mMap.setOnInfoWindowClickListener { marker ->

            openMarker(marker)

        }




        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (checkLocPermission()) {
            noTracking()
        }

    }

        fun checkLocPermission(): Boolean {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    1
                )
            } else {
                return true
            }
            return false
        }

    private fun getUserLocation():Location?{
        if(checkLocPermission()) {
            locationManager!!.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, mintime, 1f, locationListener)
            var lastLoc = locationManager!!.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            return lastLoc
        }
        return null
    }

    fun trackingStart() {
        locationManager.removeUpdates(locationListener)
        locationListener = object :LocationListener{
            override fun onLocationChanged(location: Location?) {
                mMap.clear()
                var loc = LatLng(location!!.latitude, location!!.longitude)
                var marker = mMap.addMarker(MarkerOptions().position(loc).title("Current Location"))
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 15f))
                FirebaseDb.addLoc(loc)
                FirebaseDb.changeLoc(loc)
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

            override fun onProviderEnabled(provider: String?) {}

            override fun onProviderDisabled(provider: String?) {}

        }
        getUserLocation()
    }

    fun noTracking(){
        if(locationListener!=null) {
            locationManager.removeUpdates(locationListener)
        }
        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location?) {
                mMap.clear()
                var loc = LatLng(location!!.latitude, location!!.longitude)
                var marker = mMap.addMarker(MarkerOptions().position(loc).title("Current Location"))
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 15f))
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

            override fun onProviderEnabled(provider: String?) {}

            override fun onProviderDisabled(provider: String?) {}

        }
        getUserLocation()
    }

    fun initTrackUserListener(){

        var image:Bitmap = BitmapFactory.decodeResource(resources,R.drawable.next)
        trackUserListener = object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {}

            override fun onDataChange(p0: DataSnapshot) {
                println("trackUserListener Triggered")
                var lat = p0.child("lat").getValue() as Double
                var lng = p0.child("lng").getValue() as Double

                if(userlocation!=null) {
                    userlocation!!.remove()
                }
                println("new Location $lat $lng")

                if (prevLocation == null) {
                    var currentLoc = LatLng(
                        lat,
                        lng
                    )
                    prevLocation = mMap.addMarker(MarkerOptions().position(currentLoc).title("Current Location")
                        .icon(BitmapDescriptorFactory.fromBitmap(image)))
                    mMap.animateCamera(CameraUpdateFactory.newLatLng(currentLoc))
                } else {


                    var currentLoc = LatLng(lat, lng)
                    var animator = getAnimator(prevLocation!!,currentLoc)
                    var bearing : Float = bearingBetweenLocations(prevLocation!!.position, currentLoc)

                    prevLocation!!.rotation = bearing
                    animator.addListener(object : Animator.AnimatorListener{
                        override fun onAnimationRepeat(animation: Animator?) {}

                        override fun onAnimationEnd(animation: Animator?) {
                            prevLocation!!.remove()
                            prevLocation =
                                mMap.addMarker(MarkerOptions().position(currentLoc).title("CurrentLocation")
                                    .icon(BitmapDescriptorFactory.fromBitmap(image)).rotation(bearing))
                            mMap.animateCamera(CameraUpdateFactory.newLatLng(currentLoc))
                        }

                        override fun onAnimationCancel(animation: Animator?) {}

                        override fun onAnimationStart(animation: Animator?) {}

                    })
                    animator!!.start()
                }

            }

        }
    }

    fun trackUserStart(flag:Boolean){

        if (trackUserListener != null) {
        if(flag) {

                locationManager.removeUpdates(locationListener)
                FirebaseDb.getReference().child("CurrentLocation")
                    .addValueEventListener(trackUserListener!!)
                println("Track User Triggering")

        }else{

                FirebaseDb.getReference().child("Current Location").removeEventListener(trackUserListener!!)
                println("Track User Disabled")
                getUserLocation()

        }
        }else{
            initTrackUserListener()
            trackUserStart(flag)
        }
    }

    fun getAnimator(marker:Marker,finalPosition:LatLng):ObjectAnimator{
        val typeEvaluator = TypeEvaluator<LatLng> { fraction, startValue, endValue ->
            LatLngInterpolator.Spherical().interpolate(
                fraction,
                startValue,
                endValue
            )
        }
        val property = Property.of(Marker::class.java, LatLng::class.java, "position")
        var animator = ObjectAnimator.ofObject(marker, property, typeEvaluator, finalPosition)
        animator!!.duration = 2000

        return animator

    }

    fun setPolyLines(locationList:ArrayList<LatLng>) {

        if (!locationList.isEmpty()) {
            var polyLineOptions = PolylineOptions()
            .addAll(locationList)
            .width(5f)
            .color(Color.BLUE);
            polyLine = mMap.addPolyline(polyLineOptions);
        }
    }

    fun getLocList(flag:Boolean){

        if(pathListener!=null) {
        if(flag){

//                var listt = HashMap<String, LatLng>()
                FirebaseDb.getReference().addValueEventListener(pathListener!!)
            }
            else{
                FirebaseDb.getReference().removeEventListener(pathListener!!)
                if(polyLine!=null){
//                    polyLine!!.remove()
                    mMap.clear()
                    getUserLocation()
                }
            }
    }else{
            initPathListener()
            getLocList(flag)
        }
}

    fun initPathListener(){
        pathListener = object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onDataChange(p0: DataSnapshot) {

                println("datasnap"+p0)
                locList.clear()

                for(dataSnapshot in p0.children){
                    var lat :Double= dataSnapshot.child("lat").getValue() as Double
                    var lng :Double= dataSnapshot.child("lng").getValue() as Double
                    var loc = LatLng(lat,lng)
//                    listt.put(dataSnapshot.key.toString(),loc)
                    locList.add(loc)
                    println("dataSnapshot"+dataSnapshot.key)

                }

                setPolyLines(locList)

                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(locList.get(locList.size-1),15f))

            }
        }
    }

    fun getUrl(latitude:Double,longitude : Double, type:String,keyword:String,radius: Int):String{

        val googlePlaceUri = StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json")

        googlePlaceUri.append("?location=$latitude,$longitude")
        googlePlaceUri.append("&radius=$radius")
        googlePlaceUri.append("&type=$type")
        googlePlaceUri.append("&keyword=$keyword")
        googlePlaceUri.append("&key=AIzaSyCuv--P39ZI5IQVYzgVyq6uaG4iyNmMxtA")

        Log.e("url",""+googlePlaceUri)

        return ""+ googlePlaceUri
    }

    fun getImageUrl(referencee:String):String{

        val googlePhotoUri = StringBuilder("https://maps.googleapis.com/maps/api/place/photo?")
        googlePhotoUri.append("&maxwidth=500")
        googlePhotoUri.append("&photoreference="+referencee)
        googlePhotoUri.append("&key=AIzaSyCuv--P39ZI5IQVYzgVyq6uaG4iyNmMxtA")

        Log.e("url",""+googlePhotoUri)

        return ""+ googlePhotoUri
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if(requestCode == 1){
            if(grantResults.size>0){
                if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                    noTracking()
                }
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    fun getNearbyPlaces(url:String){

        mMap.clear()

        mService.getNearbyPlaces(url)
            .enqueue(object : Callback<ResultData> {
                override fun onFailure(call: retrofit2.Call<ResultData>, t: Throwable) {
                    Toast.makeText(applicationContext,""+t.message,Toast.LENGTH_LONG).show()
                }

                override fun onResponse(
                    call: retrofit2.Call<ResultData>,
                    response: Response<ResultData>
                ) {
                    if (response!!.isSuccessful) {
                        var listVieww = ArrayList<String>()
                        nearbyIndexs.clear()
                        nearbyPlaces.clear()
                        currentPlace = response.body()!!
                        if (response.body()!!.results!!.size > 1) {
                            for (i in 0 until response!!.body()!!.results!!.size) {
                                val googlePlace = response.body()!!.results!![i]
                                val lat = googlePlace.geometry!!.location!!.lat
                                val lng = googlePlace.geometry!!.location!!.lng
                                var placeName = googlePlace.name
                                if (placeName == null)
                                    placeName = "Unknown"

                                nearbyPlaces.put(placeName, LatLng(lat!!, lng!!))
                                var markerr =
                                    MarkerOptions().position(LatLng(lat!!, lng!!)).title(placeName)
                                nearbyIndexs.add(LatLng(lat!!, lng!!))
                                listVieww.add(placeName)
                                mMap.addMarker(markerr)

                            }
                            openBottomList(listVieww)
                        }
                        Log.e("nearby Places", "" + nearbyPlaces)
                    }
                }
            })

    }

    fun openMarker(marker:Marker){
        if(marker.title.equals("Current Location")){}
        else {

            bottomSheetBehaviour.state = BottomSheetBehavior.STATE_COLLAPSED
            marker_infos_name.text = marker.title
            var index = nearbyIndexs.indexOf(marker.position)
            marker_infos_address.text = currentPlace.results!![index].vicinity
            marker_infos_rating.text = currentPlace.results!![index].rating.toString()
            var tagss = ""
            var tagarr = currentPlace.results!![index].types
            for (i in 0 until tagarr!!.size) {
                tagss += tagarr[i]+"\n"
            }
            marker_infos_tags.text = tagss
            marker_infos_progressBar.visibility = View.VISIBLE
            if(currentPlace.results!![index].photos!=null) {
                var url:String ?= getImageUrl(currentPlace.results!![index].photos!![0].photo_reference!!)
                marker_infos_image.setImageDrawable(null)
                getImage(marker_infos_image,marker_infos_progressBar).execute(url)
            }else{
                marker_infos_progressBar.visibility = View.GONE
                marker_infos_image.setImageDrawable(ContextCompat.getDrawable(applicationContext,R.drawable.not_found_img))
            }
            bottomsheetLayout.visibility = View.VISIBLE
            nearby_marker_lists.visibility = View.GONE
            marker_infos.visibility = View.VISIBLE
            bottomSheetBehaviour.state = BottomSheetBehavior.STATE_EXPANDED
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position,15f))
            mMap.addMarker(MarkerOptions().position(marker.position).title(marker.title))
        }
    }

    fun openMarker(index:Int){
        marker_infos_name.text = currentPlace.results!![index].name
        marker_infos_address.text = currentPlace.results!![index].vicinity
        marker_infos_rating.text = currentPlace.results!![index].rating.toString()
        var tagss = ""
        var tagarr = currentPlace.results!![index].types
        for (i in 0 until tagarr!!.size) {
            tagss += tagarr[i]+"\n"
        }
        marker_infos_tags.text = tagss
        marker_infos_progressBar.visibility = View.VISIBLE
        if(currentPlace.results!![index].photos!=null) {
            var url:String ?= getImageUrl(currentPlace.results!![index].photos!![0].photo_reference!!)
            marker_infos_image.setImageDrawable(null)
            getImage(marker_infos_image,marker_infos_progressBar).execute(url)
        }else{
            marker_infos_progressBar.visibility = View.GONE
            marker_infos_image.setImageDrawable(ContextCompat.getDrawable(applicationContext,R.drawable.not_found_img))
        }

        marker_infos_name.setOnClickListener(View.OnClickListener {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(nearbyIndexs[index],15f))
        })

        mMap.clear()

        bottomsheetLayout.visibility = View.VISIBLE
        nearby_marker_lists.visibility = View.GONE
        marker_infos.visibility = View.VISIBLE
        bottomSheetBehaviour.state = BottomSheetBehavior.STATE_EXPANDED
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(nearbyIndexs[index],15f))
        mMap.addMarker(MarkerOptions().position(nearbyIndexs[index]).title(currentPlace.results!![index].name))

    }

    fun openBottomList(listVieww:ArrayList<String>){

        /*      // Simple adapter
              var adapter = ArrayAdapter(
                  applicationContext,
                  R.layout.support_simple_spinner_dropdown_item,
                  listVieww
              )
      */

        //Custom adapter
        var adapter = bsListAdapter(this,listVieww)

        bottomListBehaviour.state = BottomSheetBehavior.STATE_COLLAPSED
        marker_listView.adapter = adapter
        bottomsheetLayout.visibility = View.VISIBLE
        bottomListBehaviour.state = BottomSheetBehavior.STATE_EXPANDED
        nearby_marker_lists.visibility = View.VISIBLE
        marker_infos.visibility = View.GONE
        marker_listView.setOnItemClickListener(AdapterView.OnItemClickListener { parent, view, position, id ->
            var mname = listVieww[position]
            var loc = nearbyPlaces.get(mname)

            openMarker(position)
        })
        marker_listView.setOnTouchListener(View.OnTouchListener { v, event ->

            v.parent.requestDisallowInterceptTouchEvent(true)
            v.onTouchEvent(event)
            true

        })
        if (userlocation != null) {
            mMap.addMarker(MarkerOptions().position(userlocation!!.position).title("Current Location"))
            mMap.animateCamera(
                CameraUpdateFactory.newLatLng(
                    userlocation!!.position
                )
            )
            mMap.animateCamera(CameraUpdateFactory.zoomTo(15f))
        }
    }

    private fun bearingBetweenLocations(latLng1:LatLng, latLng2:LatLng):Float {

    var PI:Double = 3.14159;
    var lat1:Double = latLng1.latitude * PI / 180;
    var long1:Double = latLng1.longitude * PI / 180;
    var lat2:Double = latLng2.latitude * PI / 180;
    var long2:Double = latLng2.longitude * PI / 180;

    var dLon:Double = (long2 - long1);

    var y:Double = Math.sin(dLon) * Math.cos(lat2);
    var x:Double = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);

    var brng:Double = Math.atan2(y, x);

    brng = Math.toDegrees(brng);
    brng = (brng + 360) % 360;

    return brng.toFloat();

}

    fun bottomSheetSetup(){
        initTrackUserListener()
//BottomSheetBehaviours
        TransitionManager.beginDelayedTransition(bottomsheetLayout)
        bottomSheetBehaviour = BottomSheetBehavior.from(marker_infos as NestedScrollView)
        bottomListBehaviour = BottomSheetBehavior.from(nearby_marker_lists as NestedScrollView)
        bottomSheetBehaviour.peekHeight = 280
        bottomListBehaviour.peekHeight = 230
        bottomSheetBehaviour.state = BottomSheetBehavior.STATE_HIDDEN
        bottomListBehaviour.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheetBehaviour.isHideable = true
        bottomListBehaviour.isHideable = true

        var listener = object : BottomSheetBehavior.BottomSheetCallback(){
            override fun onSlide(p0: View, p1: Float) {
                if(bottomSheetBehaviour.state == BottomSheetBehavior.STATE_EXPANDED){
                    bottomSheetBehaviour.state = BottomSheetBehavior.STATE_COLLAPSED
                }else if(bottomSheetBehaviour.state == BottomSheetBehavior.STATE_COLLAPSED){
                    bottomSheetBehaviour.state = BottomSheetBehavior.STATE_HIDDEN
                }else if(bottomSheetBehaviour.state == BottomSheetBehavior.STATE_HIDDEN){
                    bottomSheetBehaviour.state = BottomSheetBehavior.STATE_EXPANDED
                }
            }

            override fun onStateChanged(p0: View, p1: Int) {
            }
        }


        bottomSheetBehaviour.setBottomSheetCallback(listener)
//        bottomListBehaviour.setBottomSheetCallback(listListener)

        bottomSheetBehaviour.isHideable = true

    }

//    fun animateMarkerArr(locations : ArrayList<LatLng>,count:Int){
//
//        var countt = count
//        println("Startttt")
//        setPolyLines(locations)
//
//        if(locations.size>1&&count<locations.size-1) {
//            println("animateMarkerArr $count")
//            var marker = mMap.addMarker(MarkerOptions().position(locations[count]))
//            var animator = getAnimator(marker,locations[count+1])
//            animator.addListener(object :Animator.AnimatorListener{
//                override fun onAnimationRepeat(animation: Animator?) {
//
//                }
//
//                override fun onAnimationEnd(animation: Animator?) {
//                    marker.remove()
//                    countt++
//                    animateMarkerArr(locations,count)
//                }
//
//                override fun onAnimationCancel(animation: Animator?) {
//                    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//                }
//
//                override fun onAnimationStart(animation: Animator?) {
//                    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//                }
//            })
//            animator.start()
//        }
//    }

    internal class getImage (val imageView : ImageView,val progressBar: ProgressBar): AsyncTask<String,Void,String>(){

        var imagee : Bitmap?= null

        override fun doInBackground(vararg params: String?): String? {
            var urlstr = params[0]

            var url:URL = URL(urlstr)

            try{

                var inputt: InputStream = url.openConnection().getInputStream()
                imagee = BitmapFactory.decodeStream(inputt)


            }catch (e:Exception){
                e.printStackTrace()
            }

            return ""
        }

        override fun onPostExecute(result: String?) {
            imageView.setImageBitmap(imagee)
            Log.e("image Set","image Settttt"+imagee)
            progressBar.visibility = View.GONE

        }

    }

    override fun onBackPressed() {

        if(bottomsheetLayout.visibility == View.VISIBLE){
            TransitionManager.beginDelayedTransition(bottomsheetLayout)
            bottomsheetLayout.visibility = View.GONE
        }
        else {
            super.onBackPressed()
        }
    }

}

package com.example.locationtracker

import android.Manifest
import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.TypeEvaluator
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.AsyncTask
import android.os.Bundle
import android.transition.TransitionManager
import android.util.Log
import android.util.Property
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.widget.NestedScrollView
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.navigation.NavigationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_maps.*
import kotlinx.android.synthetic.main.marker_infos.*
import kotlinx.android.synthetic.main.nearby_markers_list.*
import kotlinx.android.synthetic.main.select_theme.view.*
import retrofit2.Callback
import retrofit2.Response
import java.io.InputStream
import java.net.URL


class MapsActivity : AppCompatActivity(), OnMapReadyCallback,
    NavigationView.OnNavigationItemSelectedListener {

    private lateinit var mMap: GoogleMap
    private lateinit var locationManager: LocationManager
    private var locationListener: LocationListener ?= null
    lateinit var mService: GoogleApiService
    private lateinit var bottomSheetBehaviour:BottomSheetBehavior<NestedScrollView>
    private lateinit var bottomListBehaviour:BottomSheetBehavior<NestedScrollView>
    internal lateinit var currentPlace : ResultData
    var mintime: Long = 10
    var prevLocation : Marker?= null
    var trackUserListener : ValueEventListener ?= null
    var userlocation :Marker ?= null
    var pathListener : ValueEventListener?= null
    var polyLine : Polyline?=null
    var nearbyPlaces = HashMap<String,LatLng>()
    var nearbyIndexs = ArrayList<LatLng>()
    var locList = ArrayList<LatLng>()
    var theme = ArrayList<Int>()
    var themeIndex = 0


    override fun onNavigationItemSelected(p0: MenuItem): Boolean {

        val search = navView.menu.findItem(R.id.search)
        val drawPath = navView.menu.findItem(R.id.drawPath)
        val removePath = navView.menu.findItem(R.id.removePath)
        val nearby = navView.menu.findItem(R.id.nearby)
        val trackStart = navView.menu.findItem(R.id.trackStart)
        val trackStop = navView.menu.findItem(R.id.trackStop)
        val trackUser = navView.menu.findItem(R.id.trackUser)
        val trackUserStop = navView.menu.findItem(R.id.trackUserStop)
        val theme = navView.menu.findItem(R.id.theme)

        drawerLayout.closeDrawer(GravityCompat.START)

        when(p0){
            search->{
                TransitionManager.beginDelayedTransition(searchLayout)
                searchBox.setText("")
                searchLayout.visibility = View.VISIBLE
            }
            drawPath->{
                getLocList(true)
                drawPath.isVisible = false
                removePath.isVisible = true
            }
            removePath->{
                getLocList(false)
                removePath.isVisible = false
                drawPath.isVisible = true
            }
            nearby->{
                val url = getUrl(userlocation!!.position.latitude,userlocation!!.position.longitude,"point_of_interest","",5000)
                getNearbyPlaces(url)
            }
            trackStart->{
                trackingStart()
//                trackUserStart(false)
//                trackUser.isVisible = false
                trackStart.isVisible = false
//                trackUserStop.isVisible = false
                trackStop.isVisible = true
            }
            trackStop->{
                noTracking()
                trackStart.isVisible = true
//                trackUser.isVisible = true
                trackStop.isVisible = false
            }
            trackUser->{
                trackUserStart(true)
                trackUser.isVisible = false
                trackUserStop.isVisible = true
            }
            trackUserStop->{
                trackUserStart(false)
                trackUser.isVisible = true
                trackUserStop.isVisible = false

            }
            theme->{
                chooseTheme()
            }
        }

        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        TransitionManager.beginDelayedTransition(bottomsheetLayout)

        theme.clear()
        theme.add(R.raw.standrad)
        theme.add(R.raw.dark_style)
        theme.add(R.raw.silver_style)
        theme.add(R.raw.night_style)
        theme.add(R.raw.retromap_style)
        theme.add(R.raw.aubergine_style)

        val intent = intent
        val flag = intent.getBooleanExtra("accuracy", true)

        if (!flag) {
            mintime = 90000
        }


        val drawerToggle = ActionBarDrawerToggle(this,drawerLayout,R.string.app_name,R.string.app_name)
        drawerLayout.addDrawerListener(object :DrawerLayout.DrawerListener{
            override fun onDrawerStateChanged(newState: Int) {

            }

            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
            }

            override fun onDrawerClosed(drawerView: View) {
                navbtn.visibility = View.VISIBLE
            }
            override fun onDrawerOpened(drawerView: View) {
                navbtn.visibility = View.GONE
            }
        })

        navView.setNavigationItemSelectedListener(this)

        drawerLayout.addDrawerListener(drawerToggle)


        navbtn.setOnClickListener {
            if(!drawerLayout.isDrawerOpen(GravityCompat.START)){
                navbtn.visibility = View.GONE
                drawerLayout.openDrawer(GravityCompat.START)
            }else{
                drawerLayout.closeDrawer(GravityCompat.START)
            }
        }

        searchbtn.setOnClickListener(View.OnClickListener {

            if (!searchBox.equals("")) {

                val url = getUrl(userlocation!!.position.latitude,userlocation!!.position.longitude,"",searchBox.text.toString(),5000)
                getNearbyPlaces(url)
            } else {
                val url = getUrl(userlocation!!.position.latitude,userlocation!!.position.longitude,"","",5000)
                getNearbyPlaces(url)
            }
            try {
                val keyboard = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                keyboard.hideSoftInputFromWindow(currentFocus.windowToken, 0)
            }catch (e:Exception){
                e.printStackTrace()
            }
            searchLayout.visibility = View.GONE

        })

//        drawerToggle.syncState()

        TransitionManager.beginDelayedTransition(constraintLayout)

        bottomSheetSetup()

    }


    override fun onMapReady(googleMap: GoogleMap) {

        mMap = googleMap
        mService = Common.googleApiService

        try {
            // Customise the styling of the base map using a JSON object defined
            // in a raw resource file.
            val success : Boolean = mMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            this, theme[themeIndex]))

            if (!success) {
                Log.e("Map style", "Style parsing failed.");
            }
        } catch (e:Exception) {
            Log.e("Exception in setting style", "Can't find style. Error: ", e);
        }

        userlocation = mMap.addMarker(MarkerOptions().position(LatLng(23.02,72.57)).title("Default"))

        mMap.setOnInfoWindowLongClickListener { marker ->
            if(userlocation!=null) {
                if(bottomSheetBehaviour.state == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetBehaviour.state = BottomSheetBehavior.STATE_COLLAPSED
                }
                if(bottomListBehaviour.state == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomListBehaviour.state = BottomSheetBehavior.STATE_COLLAPSED
                }
                val listt = ArrayList<LatLng>()
                listt.add(userlocation!!.position)
                listt.add(marker.position)
                mMap.clear()
                setPolyLines(listt)
                mMap.addMarker(MarkerOptions().position(marker.position).title(marker.title))
                val markerr = mMap.addMarker(MarkerOptions().position(userlocation!!.position).title("StartLocation"))
                val animator = getAnimator(markerr,marker.position)
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

        private fun checkLocPermission(): Boolean {
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
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, mintime, 1f, locationListener)
            val lastLoc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            return lastLoc
        }
        return null
    }

    private fun trackingStart() {
        locationManager.removeUpdates(locationListener)
        locationListener = object :LocationListener{
            override fun onLocationChanged(location: Location?) {
                mMap.clear()
                val loc = LatLng(location!!.latitude, location.longitude)
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

    private fun noTracking(){
        if(locationListener!=null) {
            locationManager.removeUpdates(locationListener)
        }
        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location?) {
                mMap.clear()
                val loc = LatLng(location!!.latitude, location.longitude)
                var marker = mMap.addMarker(MarkerOptions().position(loc).title("Current Location"))
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 15f))
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

            override fun onProviderEnabled(provider: String?) {}

            override fun onProviderDisabled(provider: String?) {}

        }
        getUserLocation()
    }

    private fun initTrackUserListener(){

        val image:Bitmap = BitmapFactory.decodeResource(resources,R.drawable.next)
        trackUserListener = object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {}

            override fun onDataChange(p0: DataSnapshot) {
                println("trackUserListener Triggered")
                val lat = p0.child("lat").getValue() as Double
                val lng = p0.child("lng").getValue() as Double

                if(userlocation!=null) {
                    userlocation!!.remove()
                }
                println("new Location $lat $lng")

                if (prevLocation == null) {
                    val currentLoc = LatLng(
                        lat,
                        lng
                    )
                    prevLocation = mMap.addMarker(MarkerOptions().position(currentLoc).title("Current Location")
                        .icon(BitmapDescriptorFactory.fromBitmap(image)))
                    mMap.animateCamera(CameraUpdateFactory.newLatLng(currentLoc))
                } else {


                    val currentLoc = LatLng(lat, lng)
                    val animator = getAnimator(prevLocation!!,currentLoc)
                    val bearing : Float = bearingBetweenLocations(prevLocation!!.position, currentLoc)

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
                    animator.start()
                }

            }

        }
    }

    private fun trackUserStart(flag:Boolean){

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
        val animator = ObjectAnimator.ofObject(marker, property, typeEvaluator, finalPosition)
        animator.duration = 2000

        return animator

    }

    fun setPolyLines(locationList:ArrayList<LatLng>) {

        if (locationList.isNotEmpty()) {
            val polyLineOptions = PolylineOptions()
            .addAll(locationList)
            .width(5f)
            .color(Color.BLUE);
            polyLine = mMap.addPolyline(polyLineOptions);
        }
    }

    private fun getLocList(flag:Boolean){

        if(pathListener!=null) {
        if(flag){


                FirebaseDb.getReference().addValueEventListener(pathListener!!)
            }
            else{
                FirebaseDb.getReference().removeEventListener(pathListener!!)
                if(polyLine!=null){
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
            }

            override fun onDataChange(p0: DataSnapshot) {

                println("datasnap$p0")
                locList.clear()

                for(dataSnapshot in p0.children){
                    val lat :Double= dataSnapshot.child("lat").getValue() as Double
                    val lng :Double= dataSnapshot.child("lng").getValue() as Double
                    val loc = LatLng(lat,lng)
                    locList.add(loc)
                    println("dataSnapshot"+dataSnapshot.key)

                }

                setPolyLines(locList)

                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(locList.get(locList.size-1),15f))

            }
        }
    }

    private fun getUrl(latitude:Double, longitude : Double, type:String, keyword:String, radius: Int):String{

        val googlePlaceUri = StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json")

        googlePlaceUri.append("?location=$latitude,$longitude")
        googlePlaceUri.append("&radius=$radius")
        googlePlaceUri.append("&type=$type")
        googlePlaceUri.append("&keyword=$keyword")
        googlePlaceUri.append("&key="+getString(R.string.google_maps_key))
        Log.e("url",""+googlePlaceUri)

        return ""+ googlePlaceUri
    }

    private fun getImageUrl(referencee:String):String{

        val googlePhotoUri = StringBuilder("https://maps.googleapis.com/maps/api/place/photo?")
        googlePhotoUri.append("&maxwidth=500")
        googlePhotoUri.append("&photoreference=$referencee")
        googlePhotoUri.append("&key="+getString(R.string.google_maps_key))

        Log.e("url",""+googlePhotoUri)

        return ""+ googlePhotoUri
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if(requestCode == 1){
            if(grantResults.isNotEmpty()){
                if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                    noTracking()
                }
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun getNearbyPlaces(url:String){

        mService.getNearbyPlaces(url)
            .enqueue(object : Callback<ResultData> {
                override fun onFailure(call: retrofit2.Call<ResultData>, t: Throwable) {
                    Toast.makeText(applicationContext,""+t.message,Toast.LENGTH_LONG).show()
                    Log.e("Nearby Places Failure",t.message + "\n"+t)
                }

                override fun onResponse(
                    call: retrofit2.Call<ResultData>,
                    response: Response<ResultData>
                ) {
                    if (response.isSuccessful) {
                        mMap.clear()
                        var listVieww = ArrayList<String>()
                        nearbyIndexs.clear()
                        nearbyPlaces.clear()
                        currentPlace = response.body()!!
                        if (response.body()!!.results!!.size > 1) {
                            for (i in 0 until response.body()!!.results!!.size) {
                                val googlePlace = response.body()!!.results!![i]
                                val lat = googlePlace.geometry!!.location!!.lat
                                val lng = googlePlace.geometry!!.location!!.lng
                                var placeName = googlePlace.name
                                if (placeName == null)
                                    placeName = "Unknown"

                                nearbyPlaces.put(placeName, LatLng(lat!!, lng!!))
//                                var markerr = MarkerOptions().position(LatLng(lat!!, lng!!)).title(placeName)
                                nearbyIndexs.add(LatLng(lat, lng))
                                listVieww.add(placeName)
                                mMap.addMarker(MarkerOptions().position(LatLng(lat, lng)).title(placeName).visible(true))
                            }
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userlocation!!.position,20f))
                            openBottomList(listVieww)
                        }
                        Log.e("nearby Places", "" + nearbyPlaces)
                    }
                    else{
                        Toast.makeText(applicationContext,""+response.body(),Toast.LENGTH_LONG).show()
                    }
                }
            })

    }

    private fun openMarker(marker:Marker){
        if(!marker.title.equals("Current Location")){

            bottomSheetBehaviour.state = BottomSheetBehavior.STATE_COLLAPSED
            marker_infos_name.text = marker.title
            val index = nearbyIndexs.indexOf(marker.position)
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
                val url:String ?= getImageUrl(currentPlace.results!![index].photos!![0].photo_reference!!)
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
            val url:String ?= getImageUrl(currentPlace.results!![index].photos!![0].photo_reference!!)
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

        //Custom adapter
        val adapter = bsListAdapter(this,listVieww)

        bottomListBehaviour.state = BottomSheetBehavior.STATE_COLLAPSED
        marker_listView.adapter = adapter
        bottomsheetLayout.visibility = View.VISIBLE
        bottomListBehaviour.state = BottomSheetBehavior.STATE_EXPANDED
        nearby_marker_lists.visibility = View.VISIBLE
        marker_infos.visibility = View.GONE
        marker_listView.setOnItemClickListener(AdapterView.OnItemClickListener { parent, view, position, id ->
            val mname = listVieww[position]
            nearbyPlaces[mname]

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

    val PI = 3.14159;
    val lat1:Double = latLng1.latitude * PI / 180;
    val long1:Double = latLng1.longitude * PI / 180;
    val lat2:Double = latLng2.latitude * PI / 180;
    val long2:Double = latLng2.longitude * PI / 180;

    val dLon:Double = (long2 - long1);

    val y:Double = Math.sin(dLon) * Math.cos(lat2);
    val x:Double = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);

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

         val listener = object : BottomSheetBehavior.BottomSheetCallback(){
            override fun onSlide(p0: View, p1: Float) {
                when {
                    bottomSheetBehaviour.state == BottomSheetBehavior.STATE_EXPANDED -> bottomSheetBehaviour.state = BottomSheetBehavior.STATE_COLLAPSED
                    bottomSheetBehaviour.state == BottomSheetBehavior.STATE_COLLAPSED -> bottomSheetBehaviour.state = BottomSheetBehavior.STATE_HIDDEN
                    bottomSheetBehaviour.state == BottomSheetBehavior.STATE_HIDDEN -> bottomSheetBehaviour.state = BottomSheetBehavior.STATE_EXPANDED
                }
            }

            override fun onStateChanged(p0: View, p1: Int) {
            }
        }
        bottomSheetBehaviour.setBottomSheetCallback(listener)
        bottomSheetBehaviour.isHideable = true

    }

    private fun chooseTheme(){

        var index = themeIndex

        val themeView = View.inflate(this,R.layout.select_theme,null)
        themeView.themeBox.check(when(themeIndex){
            0->R.id.standard
            1->R.id.dark
            2->R.id.silver
            3->R.id.night
            4->R.id.retro
            5->R.id.aubergine
            else -> R.id.standard
        })
        themeView.themeBox.setOnCheckedChangeListener { _, checkedId ->

            when(checkedId){
                R.id.standard-> index = 0
                R.id.dark->index = 1
                R.id.silver->index = 2
                R.id.night -> index = 3
                R.id.retro -> index = 4
                R.id.aubergine-> index = 5
            }

            try {
                val success : Boolean = mMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                        this, theme[index]))
                if (!success) {
                    Log.e("Map style", "Style parsing failed.");
                }
            } catch (e:Exception) {
                Log.e("Exception in setting style", "Can't find style. Error: ", e);
            }
        }

        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle("Select Theme")
        alertDialog.setView(themeView)
        alertDialog.setPositiveButton("APPLY", DialogInterface.OnClickListener { dialog, which ->

            themeIndex = index

            Toast.makeText(applicationContext,"Theme Changed",Toast.LENGTH_SHORT).show()
        })
        alertDialog.setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, which ->

            if(themeIndex!=index){
                try {
                    // Customise the styling of the base map using a JSON object defined
                    // in a raw resource file.
                    val success : Boolean = mMap.setMapStyle(
                        MapStyleOptions.loadRawResourceStyle(
                            this, theme[themeIndex]))

                    if (!success) {
                        Log.e("Map style", "Style parsing failed.");
                    }
                } catch (e:Exception) {
                    Log.e("Exception in setting style", "Can't find style. Error: ", e);
                }

            }

        })



        alertDialog.show()

    }


    internal class getImage (val imageView : ImageView,val progressBar: ProgressBar): AsyncTask<String,Void,String>(){

        var imagee : Bitmap?= null

        override fun doInBackground(vararg params: String?): String? {
            val urlstr = params[0]

            val url = URL(urlstr)

            try{

                val inputt: InputStream = url.openConnection().getInputStream()
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

        if(drawerLayout.isDrawerOpen(GravityCompat.START)){
          drawerLayout.closeDrawer(GravityCompat.START)
        } else if(bottomsheetLayout.visibility == View.VISIBLE){
            TransitionManager.beginDelayedTransition(bottomsheetLayout)
            bottomsheetLayout.visibility = View.GONE
        }else if(searchLayout.visibility ==View.VISIBLE){
            searchLayout.visibility = View.GONE
        }
        else {
            super.onBackPressed()
        }
    }

}

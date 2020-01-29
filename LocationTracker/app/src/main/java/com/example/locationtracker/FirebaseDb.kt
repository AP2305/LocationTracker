package com.example.locationtracker

import android.content.Context
import android.icu.text.SimpleDateFormat
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.gson.JsonArray
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class FirebaseDb {

    companion object FirebaseDb {

        private var database: FirebaseDatabase = FirebaseDatabase.getInstance()
        private var myRef: DatabaseReference
        private var myRefPath: DatabaseReference

        init {
            myRef = database.getReference("/user/Location")
            myRefPath = database.getReference("/user/Path")
//            auth = FirebaseAuth.getInstance()
        }

        fun addLoc(myLocation: LatLng) {

            val userLocations = Location(
                myLocation.latitude,
                myLocation.longitude
            )

            myRef.child(getCurrentTime()).setValue(userLocations)
        }

        fun changeLoc(myLocation: LatLng){
            val userLocations = Location(
                myLocation.latitude,
                myLocation.longitude
            )
            myRef.child("CurrentLocation").setValue(userLocations)
        }

        fun getReference():DatabaseReference{
            return myRef
        }

        fun getReferencePath():DatabaseReference{
            return myRefPath
        }

        private fun getCurrentTime(): String {

            val date = Calendar.getInstance().time
            val strdate = date.toString("yyyy-MM-dd HH:mm:ss")

            return strdate

        }

        private fun Date.toString(format: String, locale: Locale = Locale.getDefault()): String {
            val formatter = SimpleDateFormat(format, locale)
            return formatter.format(this)
        }
    }
}

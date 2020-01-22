package com.example.locationtracker

import com.google.android.gms.maps.model.LatLng

class Location
{

    constructor(){}
    constructor(lat:Double,lng: Double){
        this.lat = lat.toDouble()
        this.lng = lng.toDouble()
    }

    var lat : Double ?= 0.0
    var lng : Double ?= 0.0
}

class Northeast
{
    var lat : Double ?= 0.0
    var lng : Double ?= 0.0
}

class Southwest
{
    var lat : Double ?= 0.0
    var lng : Double ?= 0.0
}

class Viewport
{
    var northeast : Northeast ?= null
    var southwest : Southwest ?= null
}

class Geometry
{
    var viewport:Viewport?= null
    var location : Location ?= null
}

class OpeningHours
{
    var open_now :Boolean ?= null
}

class Photos
{
    var height : Int ?= 0
    var html_attributions : ArrayList<String>?= null
    var photo_reference : String ?= null
    var width : Int ?= 0

}

class PlusCode
{
    var compound_code : String ?= null
    var global_code : String ?= null
}

class Result
{

    var name : String ?= null
    var icon: String ?= null
    var geometry : Geometry ?= null
    var id : String ?= null
    var photos : ArrayList<Photos> ?= null
    var place_id : String?= null
    var price_level :Int = 0
    var rating : Double = 0.0
    var reference : String ?= null
    var scope : String ?= null
    var types : Array<String>?= null
    var vicinity : String ?= null
    var opening_hours : OpeningHours?= null


}
package com.example.mining

data class LatLng(val latitude: Double, val longitude: Double)

data class Listing(
    val id: Int, val title: String, val sub: String, val price: Int,
    val category: String, val image: Int, val seller: String, val info: String
)
data class Place(val id: Int, val name: String, val kind: String, val pos: LatLng)
data class Msg(val mine: Boolean, val text: String)

val CATEGORIES = listOf("All", "Electronic", "Electric", "Wanted", "Car rentals", "Drilling")

val LISTINGS = listOf(
    Listing(1, "Gold detection machine", "CTX3030", 5000, "Electronic", R.drawable.ctx3030, "Ali Ibrahim", "Manufacturer: Loop Techno Systems, India · Year: 2018"),
    Listing(2, "Electric Jack Hammer", "1700 Watt Light Weight", 3000, "Electric", R.drawable.jackhammer, "Mariem Salem", "Used 1 season · includes carry case"),
    Listing(3, "Gold Washing Plant", "Gold Wash Process & Equipment", 50000, "Drilling", R.drawable.washing_plant, "Sidi Mohamed", "Mobile plant · 2 tons/hour"),
    Listing(4, "4x4 Pickup (rental)", "Per day, with driver", 120, "Car rentals", R.drawable.pickup, "Cheikh Ould", "Gwangju pickup · fuel extra"),
    Listing(5, "Wanted: Metal detector", "Budget up to 2000 USD", 2000, "Wanted", R.drawable.wanted_detector, "Fatimetou B.", "Looking for good condition, any brand")
)

// Sample places around Gwangju (approximate coordinates, replace with real ones)
val PLACES = listOf(
    Place(1, "Hwasun Mining Area", "Mines", LatLng(35.0640, 126.9850)),
    Place(2, "Naju Mineral Site (sample)", "Mines", LatLng(35.0160, 126.7110)),
    Place(3, "Yangdong Market", "Markets", LatLng(35.1535, 126.9035)),
    Place(4, "Songjeong Market", "Markets", LatLng(35.1389, 126.7919))
)

// Fallback position (Gwangju centre) until real GPS arrives
val MY_POS = LatLng(35.1595, 126.8526)
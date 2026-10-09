@file:OptIn(ExperimentalMaterial3Api::class)

package ru.j1zwelt.kentradar

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.graphics.withTranslation
import androidx.core.net.toUri
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import org.maplibre.compose.camera.CameraAnimation
import org.maplibre.compose.camera.CameraMoveReason
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraUpdate
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.interaction.ClickEvent
import org.maplibre.compose.interaction.ClickResult
import org.maplibre.compose.interaction.MapInteractions
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.map.MapState
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.Geometry
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import ru.j1zwelt.kentradar.data.Tag
import ru.j1zwelt.kentradar.data.User
import ru.j1zwelt.kentradar.location.LocationHelper
import ru.j1zwelt.kentradar.location.SharingService
import ru.j1zwelt.kentradar.ui.theme.KentRadarTheme
import java.io.ByteArrayOutputStream
import kotlin.math.ceil
import kotlin.time.Duration.Companion.seconds

var currentUser by mutableStateOf(Firebase.auth.currentUser)
var currentAuthScreen by mutableStateOf("sign_in")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            KentRadarTheme {
                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) {}

                LaunchedEffect(Unit) {
                    val permissionsToRequest = mutableListOf<String>()

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val hasNotificationPermission = ContextCompat.checkSelfPermission(
                            this@MainActivity, Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED

                        if (!hasNotificationPermission) {
                            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }

                    val hasLocationPermission = ContextCompat.checkSelfPermission(
                        this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                    if (!hasLocationPermission) {
                        permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
                    }

                    if (permissionsToRequest.isNotEmpty()) {
                        launcher.launch(permissionsToRequest.toTypedArray())
                    }
                }

                if (currentUser != null) KentRadarApp()
                else AuthScreen()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (currentUser != null) {
            val database = Firebase.database
            database.getReference("${currentUser!!.uid}/status").setValue(1)
        }
    }

    override fun onPause() {
        super.onPause()
        if (currentUser != null) {
            val database = Firebase.database
            database.getReference("${currentUser!!.uid}/status").setValue(0)
        }
    }
}

@Composable
fun KentRadarApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.MAP) }

    //General
    val context = LocalContext.current
    val locationHelper = LocationHelper(context)

    val database = Firebase.database
    val myReference = database.getReference(currentUser!!.uid)

    val sPrefs = context.getSharedPreferences("sPrefs", MODE_PRIVATE)

    val friends = remember { mutableStateListOf<User>() }
    val selectFriend = remember { mutableStateOf<User?>(null) }
    val selectTag = remember { mutableStateOf<Tag?>(null) }
    val sharingLocation = remember { mutableStateOf(sPrefs.getBoolean("sharingLocation", true)) }

    val locationManager =
        remember { locationHelper.context.getSystemService(Context.LOCATION_SERVICE) as LocationManager }
    val isGpsActive = remember {
        mutableStateOf(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
    }

    val position = remember { mutableStateOf<Position?>(null) }

    //Service
    if (currentUser != null) {
        LaunchedEffect(sharingLocation.value, isGpsActive.value) {
            if (isGpsActive.value) {
                val hasFinePermission = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                if (hasFinePermission) {
                    locationHelper.addLocationUpdateListener {
                        position.value = Position(it.longitude, it.latitude)
                    }
                }

                val intent = Intent(context, SharingService::class.java).apply {
                    action =
                        if (sharingLocation.value) SharingService.ACTION_START else SharingService.ACTION_STOP
                }

                if (sharingLocation.value) context.startForegroundService(intent)
                else context.startService(intent)
            }

            sPrefs.edit { putBoolean("sharingLocation", sharingLocation.value) }
        }
    }

    //Map
    val mapStyle = if (isSystemInDarkTheme()) {
        "https://tiles.openfreemap.org/styles/fiord"
    } else {
        "https://tiles.openfreemap.org/styles/liberty"
    }
    val mapState = rememberMapState(
        baseStyle = BaseStyle.Uri(mapStyle), initialCameraPosition = CameraPosition(zoom = 13.0)
    ) {
        //My marker
        if (position.value != null) {
            val icon = if (isGpsActive.value) R.drawable.ic_my_map_location
            else R.drawable.ic_my_map_location_search

            Marker(
                "my-location", painterResource(icon), position.value!!
            )
        }

        // Friends markers
        val context = LocalContext.current
        val density = LocalDensity.current
        val sizeInPixels = remember(density) { with(density) { 30.dp.roundToPx() } }

        val defaultBitmapAvatar = remember(sizeInPixels) {
            val drawable = ContextCompat.getDrawable(context, R.drawable.ic_account_box)
            val output = createBitmap(sizeInPixels, sizeInPixels)
            if (drawable != null) {
                val canvas = Canvas(output)
                drawable.setBounds(0, 0, sizeInPixels, sizeInPixels)
                drawable.draw(canvas)
            }

            output.toCircleBitmap()
        }

        val myFriends =
            friends.filter { it.isFriend && it.latitude != 0.0 && it.longitude != 0.0 }
        myFriends.forEach { friend ->
            key(friend) {
                val markerPainter = remember(friend.avatarBitmap) {
                    val currentAvatar = friend.avatarBitmap
                    val finalBitmap =
                        currentAvatar?.asAndroidBitmap()?.toCircleBitmap() ?: defaultBitmapAvatar

                    BitmapPainter(finalBitmap.asImageBitmap())
                }

                Marker(
                    "marker-${friend.uid}",
                    markerPainter,
                    Position(friend.longitude, friend.latitude)
                ) {
                    selectFriend.value = friend
                    ClickResult.Consume
                }

                TagMarker("tag-${friend.uid}", database.getReference("${friend.uid}/tag"), selectTag)
            }
        }

        //My tag marker
        TagMarker("my-tag", myReference.child("tag"), selectTag)
    }

    //Friends
    val myFriendsPath = "${currentUser!!.uid}/friends"
    val myFriendsRef = database.getReference(myFriendsPath)

    LaunchedEffect(Unit) {
        myFriendsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(p0: DataSnapshot) {
                if (p0.exists()) for (child in p0.children) {
                    val userUid = child.key
                    val isFriend = child.value
                    val userRef = database.getReference("$userUid")

                    userRef.addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(p1: DataSnapshot) {
                            val checkFriendRef = myFriendsRef.child(userUid!!)
                            checkFriendRef.get().addOnSuccessListener { friendshipSnapshot ->
                                if (!friendshipSnapshot.exists()) return@addOnSuccessListener

                                val userName = p1.child("userName").value.toString()
                                val name = p1.child("name").value.toString()
                                val avatarBase64 = p1.child("avatar").value.toString()
                                val status = p1.child("status").value.toString().toInt()
                                val latitude = p1.child("latitude").value.toString().toDouble()
                                val longitude = p1.child("longitude").value.toString().toDouble()

                                val user = User(
                                    userUid,
                                    userName,
                                    name,
                                    status,
                                    latitude,
                                    longitude,
                                    isFriend.toString().toBoolean()
                                )

                                CoroutineScope(Dispatchers.IO).launch {
                                    if (avatarBase64.isNotEmpty()) {
                                        val avatarBitmap = avatarBase64.decodeBase64Image()
                                        user.avatarBitmap = avatarBitmap
                                    }

                                    withContext(Dispatchers.Main) {
                                        val existingIndex =
                                            friends.indexOfFirst { it.uid == user.uid }
                                        if (existingIndex != -1) {
                                            friends[existingIndex] = user
                                        } else {
                                            friends.add(user)
                                        }
                                    }
                                }
                            }
                        }

                        override fun onCancelled(p0: DatabaseError) {
                            p0.toException().printStackTrace()
                        }
                    })
                }
            }

            override fun onCancelled(p0: DatabaseError) {
                p0.toException().printStackTrace()
            }
        })
    }

    //Profile
    val loadingStr = stringResource(R.string.loading)
    val userName = remember { mutableStateOf(loadingStr) }
    val oldUserName = remember { mutableStateOf("") }
    val name = remember { mutableStateOf(loadingStr) }
    val oldName = remember { mutableStateOf("") }
    val avatar = remember { mutableStateOf<ImageBitmap?>(null) }
    val oldAvatar = remember { mutableStateOf<ImageBitmap?>(null) }

    //Main UI
    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            painterResource(it.icon), contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it })
            }
        }) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            when (currentDestination) {
                AppDestinations.MAP -> {
                    Map(
                        modifier = Modifier.padding(innerPadding),
                        locationHelper,
                        mapState,
                        isGpsActive,
                        sharingLocation,
                        position,
                        selectFriend,
                        selectTag
                    )
                }

                AppDestinations.FRIENDS -> {
                    Friends(
                        modifier = Modifier.padding(innerPadding), friends, selectFriend
                    ) { currentDestination = AppDestinations.MAP }
                }

                AppDestinations.PROFILE -> {
                    Profile(
                        modifier = Modifier.padding(innerPadding),
                        userName,
                        oldUserName,
                        name,
                        oldName,
                        avatar,
                        oldAvatar
                    )
                }
            }
        }
    }
}

@Composable
fun Marker(
    id: String,
    drawable: Painter,
    coordinates: Position,
    minZoom: Float = 0.0f,
    onClick: (ClickEvent.(List<Feature<Geometry, JsonObject?>>) -> ClickResult)? = null
) {
    val point = Point(coordinates)
    val geoSource = rememberGeoJsonSource(GeoJsonData.Features(point))

    SymbolLayer(
        id = id,
        source = geoSource,
        iconImage = image(drawable),
        minZoom = minZoom,
        onClick = onClick
    )
}

@Composable
fun TagMarker(id: String, reference: DatabaseReference, selectTag: MutableState<Tag?>) {
    val tag = remember { mutableStateOf<Tag?>(null) }

    reference.addValueEventListener(object : ValueEventListener {
        override fun onDataChange(p0: DataSnapshot) {
            if (p0.exists() && p0.value != null) {
                val longitude = "${p0.child("longitude").value}".toDouble()
                val latitude = "${p0.child("latitude").value}".toDouble()
                val text = "${p0.child("text").value}"

                tag.value = Tag(text, Position(longitude, latitude))
            } else tag.value = null
        }

        override fun onCancelled(p0: DatabaseError) {
            p0.toException().printStackTrace()
        }
    })

    key(tag.value) {
        val currentTag = tag.value
        if (currentTag != null) {
            val tagPainter = BitmapPainter(currentTag.text.toTagBitmap())
            Marker(id, tagPainter, currentTag.position, 10.5f) {
                selectTag.value = currentTag
                ClickResult.Consume
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    MAP("Map", R.drawable.ic_map), FRIENDS("Friends", R.drawable.ic_friends), PROFILE(
        "Profile", R.drawable.ic_account_box
    )
}

@Composable
fun AuthScreen() {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        when (currentAuthScreen) {
            "sign_in" -> SingIn(Modifier.padding(innerPadding))
            "register" -> Register(Modifier.padding(innerPadding))
        }
    }
}

@Composable
fun Map(
    modifier: Modifier = Modifier,
    locationHelper: LocationHelper,
    mapState: MapState,
    isGpsActive: MutableState<Boolean>,
    sharingLocation: MutableState<Boolean>,
    position: MutableState<Position?>,
    selectFriend: MutableState<User?>,
    selectTag: MutableState<Tag?>
) {
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberBottomSheetScaffoldState()
    val context = locationHelper.context

    val database = Firebase.database

    var isMapCentered by remember { mutableStateOf(false) }

    val status = remember { mutableIntStateOf(0) }
    val charge = remember { mutableIntStateOf(0) }
    val temperature = remember { mutableFloatStateOf(0.0f) }

    val tagLatitude = remember { mutableDoubleStateOf(0.0) }
    val tagLongitude = remember { mutableDoubleStateOf(0.0) }
    var showAddTagDialog by remember { mutableStateOf(false) }
    var tagText by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    val errorMessage = stringResource(R.string.no_applications_found)

    DisposableEffect(key1 = Unit) {
        val filter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
        val gpsReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == LocationManager.PROVIDERS_CHANGED_ACTION) {
                    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                    isGpsActive.value = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                }
            }
        }

        ContextCompat.registerReceiver(
            context, gpsReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED
        )

        onDispose { context.unregisterReceiver(gpsReceiver) }
    }

    LaunchedEffect(isMapCentered, position.value) {
        if (isMapCentered) {
            if (position.value != null) {
                mapState.animateCamera(position.value!!)
            }
        }
    }

    LaunchedEffect(selectFriend.value) {
        val currentFriend = selectFriend.value
        if (currentFriend != null) {
            scaffoldState.bottomSheetState.expand()
            mapState.animateCamera(Position(currentFriend.longitude, currentFriend.latitude))

            isMapCentered = false
        }
    }

    LaunchedEffect(selectTag.value) {
        val currentTag = selectTag.value
        if (currentTag != null) {
            mapState.animateCamera(currentTag.position, 14.0)

            isMapCentered = false
        }
    }

    DisposableEffect(selectFriend.value) {
        if (selectFriend.value == null) return@DisposableEffect onDispose { }

        val reference = database.getReference(selectFriend.value!!.uid)

        val statusListener = object : ValueEventListener {
            override fun onDataChange(p0: DataSnapshot) {
                status.intValue = if (p0.exists()) "${p0.value}".toInt() else 0
            }

            override fun onCancelled(p0: DatabaseError) {
                p0.toException().printStackTrace()
            }
        }

        val chargeListener = object : ValueEventListener {
            override fun onDataChange(p0: DataSnapshot) {
                charge.intValue = if (p0.exists()) "${p0.value}".toInt() else 0
            }

            override fun onCancelled(p0: DatabaseError) {
                p0.toException().printStackTrace()
            }
        }

        val temperatureListener = object : ValueEventListener {
            override fun onDataChange(p0: DataSnapshot) {
                temperature.floatValue = if (p0.exists()) "${p0.value}".toFloat() else 0.0f
            }

            override fun onCancelled(p0: DatabaseError) {
                p0.toException().printStackTrace()
            }
        }

        reference.child("status").addValueEventListener(statusListener)
        reference.child("battery/charge").addValueEventListener(chargeListener)
        reference.child("battery/temperature").addValueEventListener(temperatureListener)

        onDispose {
            reference.child("status").removeEventListener(statusListener)
            reference.child("battery/charge").removeEventListener(chargeListener)
            reference.child("battery/temperature").removeEventListener(temperatureListener)
        }
    }

    LaunchedEffect(mapState) {
        snapshotFlow { mapState.cameraMoveReason }.collectLatest { reason ->
            if (reason == CameraMoveReason.GESTURE) {
                isMapCentered = false
                selectFriend.value = null
                selectTag.value = null
                scaffoldState.bottomSheetState.partialExpand()
            }
        }
    }

    LaunchedEffect(key1 = scaffoldState.bottomSheetState.targetValue) {
        if (scaffoldState.bottomSheetState.targetValue == SheetValue.PartiallyExpanded) {
            selectFriend.value = null
            selectTag.value = null
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 0.dp,
        sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        sheetDragHandle = { BottomSheetDefaults.DragHandle() },
        sheetContent = {
            val friend = selectFriend.value

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (friend != null) {
                    Text(
                        text = friend.name,
                        style = MaterialTheme.typography.headlineMedium,
                    )

                    Text(
                        text = if (status.intValue == 1) stringResource(R.string.online) else stringResource(
                            R.string.offline
                        ), color = if (status.intValue == 1) Color.Green else Color.Gray
                    )

                    Spacer(Modifier.height(8.dp))

                    Row {
                        Icon(
                            painterResource(R.drawable.ic_charge),
                            contentDescription = stringResource(R.string.charge)
                        )
                        Text("${if (charge.intValue != 0) charge.intValue else "--"}%")

                        Spacer(Modifier.width(8.dp))

                        Icon(
                            painterResource(R.drawable.ic_temperature),
                            contentDescription = stringResource(R.string.temperature)
                        )
                        Text("${if (temperature.floatValue != 0.0f) temperature.floatValue else "--.-"}°C")
                    }

                    Spacer(Modifier.height(16.dp))

                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(), onClick = {
                            try {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    "geo:0,0?q=${friend.latitude},${friend.longitude}".toUri()
                                )
                                context.startActivity(intent)
                            } catch (e: ActivityNotFoundException) {
                                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                e.printStackTrace()
                            } finally {
                                scope.launch {
                                    isMapCentered = false
                                    selectFriend.value = null
                                    scaffoldState.bottomSheetState.partialExpand()
                                }
                            }
                        }) {
                        Text(stringResource(R.string.build_a_route_to, friend.name))
                    }
                }
            }
        }) {
        Box(modifier = modifier.fillMaxSize()) {
            MaplibreMap(
                modifier = Modifier.fillMaxSize(),
                state = mapState,
                interactions = MapInteractions {
                    callbacks {
                        longClick {
                            onEvent { event ->
                                val position = event.position!!
                                tagLatitude.doubleValue = position.latitude
                                tagLongitude.doubleValue = position.longitude

                                showAddTagDialog = true

                                ClickResult.Consume
                            }
                        }
                    }
                })

            Switch(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                thumbContent = if (sharingLocation.value) {
                    {
                        Icon(
                            painterResource(R.drawable.ic_broadcast_location),
                            contentDescription = context.getString(R.string.broadcast_location),
                            modifier = Modifier.size(SwitchDefaults.IconSize),
                        )
                    }
                } else null,
                checked = sharingLocation.value,
                onCheckedChange = { sharingLocation.value = it })

            FloatingActionButton(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp), onClick = {
                    if (position.value != null) isMapCentered = true
                }) {
                Icon(
                    painter = if (isMapCentered) painterResource(id = R.drawable.ic_my_location)
                    else painterResource(id = R.drawable.ic_my_location_search),
                    contentDescription = stringResource(R.string.my_location),
                )
            }

            if (showAddTagDialog) {
                val maxCharLimit = 30

                AlertDialog(onDismissRequest = { showAddTagDialog = false }, title = {
                    Text(text = stringResource(R.string.adding_a_tag_to_the_map))
                }, text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = tagText,
                            onValueChange = { tagText = it },
                            label = { Text(stringResource(R.string.enter_text)) },
                            isError = isError,
                            supportingText = {
                                Text(
                                    text = "${tagText.length}/$maxCharLimit",
                                    textAlign = TextAlign.End,
                                    color = if (tagText.length > maxCharLimit) {
                                        isError = true
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        isError = false
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }, confirmButton = {
                    TextButton(
                        onClick = {
                            if (tagText != "" && tagText.length < maxCharLimit) {
                                val tagData = mapOf(
                                    "longitude" to tagLongitude.doubleValue,
                                    "latitude" to tagLatitude.doubleValue,
                                    "text" to tagText
                                )

                                database.getReference(currentUser!!.uid).child("tag")
                                    .setValue(tagData)

                                showAddTagDialog = false
                                tagText = ""
                            }
                        }) {
                        Text(stringResource(R.string.send))
                    }
                }, dismissButton = {
                    TextButton(onClick = {
                        showAddTagDialog = false
                        tagText = ""
                    }) { Text(stringResource(R.string.cancel)) }
                })
            }
        }
    }
}

@Composable
fun Friends(
    modifier: Modifier = Modifier,
    friends: SnapshotStateList<User>,
    selectFriend: MutableState<User?>,
    onNavigateToMap: () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        val errorMessage = stringResource(R.string.failed_to_accept_the_friend_request)
        val errorMessage2 = stringResource(R.string.failed_to_reject_the_friend_request)

        var showSendDialog by remember { mutableStateOf(false) }
        var removableFriend by remember { mutableStateOf<User?>(null) }
        val snackBarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

        val database = Firebase.database

        var userName by remember { mutableStateOf("") }

        LazyColumn {
            items(friends) { user ->
                User(user, onClick = {
                    selectFriend.value = user
                    onNavigateToMap()
                }, onLongClick = {
                    removableFriend = user
                }, onAcceptClick = {
                    val myRef = database.getReference("${currentUser!!.uid}/friends/${user.uid}")
                        .setValue(true)

                    myRef.addOnSuccessListener {
                        val index = friends.indexOf(user)
                        if (index != -1) friends[index] = user.copy(isFriend = true)
                        database.getReference("${user.uid}/friends/${currentUser!!.uid}")
                            .setValue(true)
                    }

                    myRef.addOnFailureListener {
                        scope.launch {
                            snackBarHostState.showSnackbar(it.localizedMessage ?: errorMessage)
                        }
                    }
                }, onDismissClick = {
                    val reference =
                        database.getReference("${currentUser!!.uid}/friends/${user.uid}")
                            .removeValue()
                    reference.addOnSuccessListener {
                        friends.remove(user)
                    }

                    reference.addOnFailureListener {
                        scope.launch {
                            snackBarHostState.showSnackbar(it.localizedMessage ?: errorMessage2)
                        }
                    }
                })
            }
        }

        FloatingActionButton(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp), onClick = {
                showSendDialog = true
            }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_add),
                contentDescription = stringResource(R.string.add_friend)
            )
        }

        if (showSendDialog) {
            var isNullUser by remember { mutableStateOf(false) }

            AlertDialog(onDismissRequest = { showSendDialog = false }, title = {
                Text(text = stringResource(R.string.sending_friend_requests))
            }, text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.enter_a_friend_s_username))

                    OutlinedTextField(
                        value = userName,
                        onValueChange = { userName = it },
                        label = { Text(stringResource(R.string.username)) },
                        isError = isNullUser,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }, confirmButton = {
                TextButton(
                    onClick = {
                        val editUserName = userName.replace("@", "").trim()

                        if (!(friends.any { it.userName == editUserName })) {
                            val searchRef = database.getReference("users/$editUserName/uid").get()

                            searchRef.addOnSuccessListener {
                                if (it.exists()) {
                                    val friendUid = it.value.toString()
                                    val friendRef =
                                        database.getReference("$friendUid/friends/${currentUser!!.uid}")
                                    friendRef.setValue(false)

                                    showSendDialog = false
                                } else isNullUser = true
                            }
                        } else isNullUser = true
                    }) {
                    Text(stringResource(R.string.send))
                }
            }, dismissButton = {
                TextButton(onClick = {
                    showSendDialog = false
                }) { Text(stringResource(R.string.cancel)) }
            })
        }

        if (removableFriend != null) {
            AlertDialog(onDismissRequest = { removableFriend = null }, title = {
                Text(text = stringResource(R.string.remove_from_friends))
            }, text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(
                            R.string.do_you_really_want_to_remove_from_friends,
                            removableFriend!!.name
                        )
                    )
                }
            }, confirmButton = {
                TextButton(
                    onClick = {
                        val myRef =
                            database.getReference("${currentUser!!.uid}/friends/${removableFriend!!.uid}")
                        val userRef =
                            database.getReference("${removableFriend!!.uid}/friends/${currentUser!!.uid}")
                        myRef.removeValue()
                        userRef.removeValue()

                        friends.removeIf { it.uid == removableFriend!!.uid }

                        removableFriend = null
                    }) {
                    Text(stringResource(R.string.remove))
                }
            }, dismissButton = {
                TextButton(onClick = {
                    removableFriend = null
                }) { Text(stringResource(R.string.cancel)) }
            })
        }

        SnackbarHost(
            hostState = snackBarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )
    }
}

@Composable
fun User(
    user: User,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onAcceptClick: () -> Unit,
    onDismissClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let {
                if (user.isFriend) {
                    it.combinedClickable(onClick = { onClick() }, onLongClick = { onLongClick() })
                } else it
            }
            .padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(54.dp)
        ) {
            Image(painter = user.avatarBitmap?.let { BitmapPainter(it) }
                ?: painterResource(id = R.drawable.ic_account_box),
                contentDescription = stringResource(id = R.string.my_photo),
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .border(
                        border = BorderStroke(
                            width = 2.dp, color = MaterialTheme.colorScheme.primary
                        ), shape = CircleShape
                    ),
                contentScale = ContentScale.Crop)

            if (user.status == 1) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .align(Alignment.BottomEnd)
                        .background(Color.Green, CircleShape)
                        .border(1.5.dp, Color.White, CircleShape)
                )
            }
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(user.name, style = MaterialTheme.typography.titleLarge)
            Text(user.userName, style = MaterialTheme.typography.titleSmall)
        }

        if (!user.isFriend) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismissClick) {
                    Icon(
                        painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.reject),
                        tint = MaterialTheme.colorScheme.error
                    )
                }

                IconButton(
                    onClick = onAcceptClick, colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Icon(
                        painterResource(R.drawable.ic_check),
                        contentDescription = stringResource(R.string.accept),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun Profile(
    modifier: Modifier = Modifier,
    userName: MutableState<String>,
    oldUserName: MutableState<String>,
    name: MutableState<String>,
    oldName: MutableState<String>,
    avatar: MutableState<ImageBitmap?>,
    oldAvatar: MutableState<ImageBitmap?>
) {
    val context = LocalContext.current

    val scope = rememberCoroutineScope()

    val sheetState = rememberModalBottomSheetState(true)
    var isSheetOpen by remember { mutableStateOf(false) }
    val snackBarHostState = remember { SnackbarHostState() }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(), onResult = {
            if (it != null) {
                selectedImageUri = it
            }
        })

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(), onResult = {
            if (it != null) {
                avatar.value = it.asImageBitmap()
                selectedImageUri = null
            }
        })

    val loadingStr = stringResource(R.string.loading)
    val errorMessage = stringResource(R.string.unknown_error)

    var userNameIsTaken by remember { mutableStateOf(false) }

    val database = Firebase.database
    val reference = database.getReference(currentUser!!.uid)

    LaunchedEffect(key1 = Unit) {
        reference.get().addOnSuccessListener {
            if (it.exists()) {
                if (userName.value == loadingStr) userName.value =
                    it.child("userName").value.toString()
                if (oldUserName.value == "") oldUserName.value = userName.value
                if (name.value == loadingStr) name.value = it.child("name").value.toString()
                if (oldName.value == "") oldName.value = name.value
            }
        }
    }

    LaunchedEffect(selectedImageUri) {
        if (selectedImageUri != null) {
            try {
                context.contentResolver.openInputStream(selectedImageUri!!).use { inputStream ->
                    avatar.value = BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    if (oldAvatar.value == null) {
        reference.child("avatar").get().addOnSuccessListener {
            val avatarBase64 = it.value.toString()
            val avatarBitmap = avatarBase64.decodeBase64Image()

            avatar.value = avatarBitmap
            oldAvatar.value = avatarBitmap
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            Image(
                painter = avatar.value?.let { BitmapPainter(it) }
                    ?: painterResource(id = R.drawable.ic_account_box),
                contentDescription = stringResource(id = R.string.my_photo),
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .border(
                        border = BorderStroke(3.dp, MaterialTheme.colorScheme.primary),
                        shape = CircleShape
                    )
                    .clickable {
                        isSheetOpen = true
                    },
                contentScale = ContentScale.FillBounds
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = userName.value,
                onValueChange = { userName.value = it },
                label = { Text(stringResource(R.string.username)) },
                leadingIcon = {
                    Text(
                        text = "@",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                isError = userNameIsTaken,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = name.value,
                onValueChange = { name.value = it },
                label = { Text(stringResource(R.string.name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (userName.value != oldUserName.value) {
                        val newUserName = userName.value.replace("@", "").trim()

                        val usersRef = database.getReference("users")
                        val newUserNameRef = usersRef.child(newUserName).get()
                        val oldUserNameRef = usersRef.child(oldUserName.value)

                        newUserNameRef.addOnSuccessListener { checkSnap ->
                            if (checkSnap.value == null) {
                                reference.child("userName").setValue(newUserName)
                                oldUserNameRef.removeValue()
                                checkSnap.ref.setValue(currentUser!!.uid)

                                oldUserName.value = userName.value
                                userNameIsTaken = false
                            } else userNameIsTaken = true
                        }

                        newUserNameRef.addOnFailureListener {
                            scope.launch {
                                snackBarHostState.showSnackbar(
                                    it.localizedMessage ?: errorMessage
                                )
                            }
                        }
                    }

                    if (name.value != oldName.value) {
                        reference.child("name").setValue(name.value)
                        oldName.value = name.value
                    }

                    if (avatar.value != oldAvatar.value) {
                        val androidBitmap = avatar.value!!.asAndroidBitmap()
                        val byteArrayOutputStream = ByteArrayOutputStream()

                        val scaledBitmap = androidBitmap.scale(160, 160)
                        scaledBitmap.compress(
                            Bitmap.CompressFormat.JPEG, 75, byteArrayOutputStream
                        )

                        val byteArray = byteArrayOutputStream.toByteArray()

                        val base64String = Base64.encodeToString(byteArray, Base64.DEFAULT)
                        val finalBase64Data = "$base64String"

                        reference.child("avatar").setValue(finalBase64Data)

                        oldAvatar.value = avatar.value
                    }
                },
                enabled = userName.value != loadingStr && name.value != loadingStr && (userName.value != oldUserName.value || name.value != oldName.value || avatar.value != oldAvatar.value)
            ) {
                Text(stringResource(R.string.save_changes))
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(), onClick = {
                    Firebase.auth.signOut()
                    currentUser = null
                }) {
                Text(stringResource(R.string.sign_out))
            }
        }

        SnackbarHost(
            hostState = snackBarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )

        if (isSheetOpen) {
            ModalBottomSheet(
                onDismissRequest = { isSheetOpen = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle() }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        modifier = Modifier.fillMaxWidth(), onClick = {
                            cameraLauncher.launch()
                            isSheetOpen = false
                        }) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_camera_alt),
                                contentDescription = stringResource(R.string.make_photo)
                            )

                            Text(stringResource(R.string.make_photo))
                        }
                    }

                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(), onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                            isSheetOpen = false
                        }) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_add_photo),
                                contentDescription = stringResource(R.string.select_from_gallery)
                            )

                            Text(
                                stringResource(R.string.select_from_gallery),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Register(modifier: Modifier = Modifier) {
    val snackBarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val database = Firebase.database
    val auth = Firebase.auth

    val errorMessage = stringResource(R.string.unknown_error)
    val errorMessage2 = stringResource(R.string.passwords_do_not_match)
    val errorMessage3 = stringResource(R.string.this_username_is_already_taken_by_another_user)

    var email by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var password1 by remember { mutableStateOf("") }
    var password2 by remember { mutableStateOf("") }

    var isTaken by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.create_new_profile),
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(id = R.string.email)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = userName,
                onValueChange = {
                    userName = it
                },
                label = { Text(stringResource(R.string.username)) },
                isError = isTaken,
                leadingIcon = {
                    Text(
                        text = "@",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = password1,
                onValueChange = { password1 = it },
                label = { Text(stringResource(id = R.string.password)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = password2,
                onValueChange = { password2 = it },
                label = { Text(stringResource(R.string.repeat_password)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            Spacer(Modifier.height(16.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (password1 == password2) {
                        val newUserName = userName.replace("@", "").trim()

                        val checkUserName = database.getReference("users/$newUserName").get()
                        checkUserName.addOnSuccessListener { checkSnap ->
                            if (checkSnap.value == null) {
                                val createUser =
                                    auth.createUserWithEmailAndPassword(email, password1)
                                createUser.addOnSuccessListener {
                                    currentUser = it.user
                                    val uid = currentUser!!.uid
                                    val userInfo = mapOf("userName" to newUserName, "name" to name)
                                    database.getReference(uid).setValue(userInfo)
                                    database.getReference("users/$newUserName/uid").setValue(uid)
                                }

                                createUser.addOnFailureListener {
                                    scope.launch {
                                        snackBarHostState.showSnackbar(
                                            it.localizedMessage ?: errorMessage
                                        )
                                    }
                                }
                            } else {
                                scope.launch {
                                    snackBarHostState.showSnackbar(
                                        errorMessage3
                                    )
                                }
                            }
                        }

                        checkUserName.addOnFailureListener {
                            scope.launch {
                                snackBarHostState.showSnackbar(
                                    it.localizedMessage ?: errorMessage
                                )
                            }
                        }
                    } else {
                        scope.launch {
                            snackBarHostState.showSnackbar(
                                message = errorMessage2
                            )
                        }
                    }
                },
                enabled = email.isNotEmpty() && userName.isNotEmpty() && password1.isNotEmpty() && password2.isNotEmpty()
            ) {
                Text(stringResource(id = R.string.register))
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(), onClick = {
                    currentAuthScreen = "sign_in"
                }) {
                Text(stringResource(R.string.sign_in))
            }
        }

        SnackbarHost(
            hostState = snackBarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )
    }
}

@Composable
fun SingIn(modifier: Modifier = Modifier) {
    val snackBarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val errorMessage = stringResource(R.string.unknown_error)

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.log_in_to_your_profile),
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(id = R.string.email)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(id = R.string.password)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            Spacer(Modifier.height(16.dp))

            Button(
                modifier = Modifier.fillMaxWidth(), onClick = {
                    val auth = Firebase.auth
                    val signIn = auth.signInWithEmailAndPassword(email, password)

                    signIn.addOnSuccessListener {
                        currentUser = it.user
                    }

                    signIn.addOnFailureListener {
                        scope.launch {
                            snackBarHostState.showSnackbar(
                                it.localizedMessage ?: errorMessage
                            )
                        }
                    }
                }, enabled = email.isNotEmpty() && password.isNotEmpty()
            ) {
                Text(stringResource(id = R.string.sign_in))
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(), onClick = {
                    currentAuthScreen = "register"
                }) {
                Text(stringResource(R.string.register))
            }
        }

        SnackbarHost(
            hostState = snackBarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )
    }
}

//Functions
private fun Bitmap.toCircleBitmap(
    size: Int = 120, borderWidth: Float = 6f
): Bitmap {
    val softwareBitmap = if (this.config == Bitmap.Config.HARDWARE) {
        this.copy(Bitmap.Config.ARGB_8888, false)
    } else this

    val scaledSrc = softwareBitmap.scale(size, size)

    val output = createBitmap(size, size)
    val canvas = Canvas(output)

    val paint = Paint().apply {
        isAntiAlias = true
    }
    val radius = size / 2f

    canvas.drawCircle(radius, radius, radius - borderWidth, paint)
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(scaledSrc, 0f, 0f, paint)

    if (borderWidth > 0) {
        val borderPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = borderWidth
            color = android.graphics.Color.WHITE
            isAntiAlias = true
        }
        canvas.drawCircle(radius, radius, radius - borderWidth / 2f, borderPaint)
    }

    return output
}

fun String.decodeBase64Image(): ImageBitmap? {
    return try {
        val imageBytes = Base64.decode(this, Base64.DEFAULT)
        val decodedBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        decodedBitmap?.asImageBitmap()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun String.toTagBitmap(): ImageBitmap {
    val paddingHorizontal = 24f
    val paddingVertical = 16f
    val interval = 0.7f
    val cornerRadius = 20f
    val maxAllowedWidth = 250

    val textPaint = TextPaint().apply {
        textSize = 40f
        isAntiAlias = true
    }

    val bgPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        isAntiAlias = true
    }

    val staticLayout = StaticLayout.Builder.obtain(this, 0, this.length, textPaint, maxAllowedWidth)
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setLineSpacing(0f, interval)
        .setIncludePad(false)
        .build()

    var maxLineWidth = 0f
    for (i in 0 until staticLayout.lineCount) {
        val lineWidth = staticLayout.getLineWidth(i)
        if (lineWidth > maxLineWidth) {
            maxLineWidth = lineWidth
        }
    }

    val finalWidth = ceil(maxLineWidth.toDouble()).toInt()
    val finalHeight = staticLayout.height

    val bitmapWidth = (finalWidth + paddingHorizontal * 2).toInt()
    val bitmapHeight = (finalHeight + paddingVertical * 2).toInt()

    val rect = RectF(0f, 0f, bitmapWidth.toFloat(), bitmapHeight.toFloat())

    val bitmap = createBitmap(bitmapWidth, bitmapHeight)
    val canvas = Canvas(bitmap)
    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)

    canvas.withTranslation(paddingHorizontal, paddingVertical) {
        staticLayout.draw(this)
    }

    return bitmap.asImageBitmap()
}

suspend fun MapState.animateCamera(position: Position, zoom: Double = 13.0) {
    this.animateCamera(
        CameraUpdate(position, zoom), CameraAnimation.Fly(duration = 3.seconds)
    )
}
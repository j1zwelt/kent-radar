package ru.j1zwelt.kentradar

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableDoubleState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraMoveReason
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.map.MapState
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import ru.j1zwelt.kentradar.data.User
import ru.j1zwelt.kentradar.location.LocationHelper
import ru.j1zwelt.kentradar.ui.theme.KentRadarTheme
import kotlin.time.Duration.Companion.seconds

var currentUser by mutableStateOf(Firebase.auth.currentUser)
var currentAuthScreen by mutableStateOf("sign_in")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KentRadarTheme {
                currentUser = Firebase.auth.currentUser
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

    val friends = remember { mutableStateListOf<User>() }
    val friend = remember { mutableStateOf<User?>(null) }

    //Map
    val locationHelper = LocationHelper(LocalContext.current)

    val locationManager =
        remember { locationHelper.context.getSystemService(Context.LOCATION_SERVICE) as LocationManager }
    val isGpsActive = remember {
        mutableStateOf(
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
            )
        )
    }

    val latitude = remember { mutableDoubleStateOf(0.0) }
    val longitude = remember { mutableDoubleStateOf(0.0) }

    val mapStyle = if (isSystemInDarkTheme()) {
        "https://tiles.openfreemap.org/styles/fiord"
    } else {
        "https://tiles.openfreemap.org/styles/liberty"
    }
    val mapState = rememberMapState(
        baseStyle = BaseStyle.Uri(mapStyle), initialCameraPosition = CameraPosition(
            target = Position(latitude = latitude.doubleValue, longitude = longitude.doubleValue),
            zoom = 13.0
        )
    ) {
        val database = Firebase.database
        val reference = database.getReference(currentUser!!.uid)

        //My marker
        if (latitude.doubleValue != 0.0 && longitude.doubleValue != 0.0) {
            val myPoint = Point(
                coordinates = Position(
                    longitude = longitude.doubleValue, latitude = latitude.doubleValue
                )
            )
            val myGeoSource = rememberGeoJsonSource(
                data = GeoJsonData.Features(myPoint)
            )

            val icon = painterResource(
                if (isGpsActive.value) R.drawable.ic_my_map_location
                else R.drawable.ic_my_map_location_search
            )

            SymbolLayer(
                id = "my-live-location-layer", source = myGeoSource, iconImage = image(icon)
            )

            reference.child("latitude").setValue(latitude.doubleValue)
            reference.child("longitude").setValue(longitude.doubleValue)
        }

        //Friends markers
        val activeFriends =
            friends.filter { it.isFriend && it.latitude != 0.0 && it.longitude != 0.0 }
        activeFriends.forEach { friend ->
            val friendPoint =
                Point(Position(longitude = friend.longitude, latitude = friend.latitude))

            val friendSource = rememberGeoJsonSource(
                data = GeoJsonData.Features(friendPoint)
            )

            SymbolLayer(
                id = "layer-${friend.uid}",
                source = friendSource,
                iconImage = image(painterResource(R.drawable.ic_friends)),
            )
        }
    }

    //Friends
    val myFriendsPath = "${currentUser!!.uid}/friends"
    val database = Firebase.database
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
                                if (!friendshipSnapshot.exists()) {
                                    return@addOnSuccessListener
                                }

                                val userName = p1.child("userName").value.toString()
                                val name = p1.child("name").value.toString()
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

                                val existingIndex = friends.indexOfFirst { it.uid == user.uid }
                                if (existingIndex != -1) {
                                    friends[existingIndex] = user
                                } else {
                                    friends.add(user)
                                }
                            }
                        }

                        override fun onCancelled(p0: DatabaseError) {
                            TODO("Not yet implemented")
                        }
                    })
                }
            }

            override fun onCancelled(p0: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }

    //Profile
    val loadingStr = stringResource(R.string.loading)
    val userName = remember { mutableStateOf(loadingStr) }
    val oldUserName = remember { mutableStateOf("") }
    val name = remember { mutableStateOf(loadingStr) }
    val oldName = remember { mutableStateOf("") }

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
                        latitude,
                        longitude,
                        friend
                    )
                }

                AppDestinations.FRIENDS -> {
                    Friends(
                        modifier = Modifier.padding(innerPadding), friends, friend
                    ) { currentDestination = AppDestinations.MAP }
                }

                AppDestinations.PROFILE -> {
                    Profile(
                        modifier = Modifier.padding(innerPadding),
                        userName,
                        oldUserName,
                        name,
                        oldName
                    )
                }
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
    latitude: MutableDoubleState,
    longitude: MutableDoubleState,
    friend: MutableState<User?>
) {
    val currentContext = locationHelper.context

    var isMapCentered by remember { mutableStateOf(false) }

    DisposableEffect(key1 = Unit) {
        val filter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
        val gpsReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == LocationManager.PROVIDERS_CHANGED_ACTION) {
                    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                    isGpsActive.value =
                        lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(
                            LocationManager.NETWORK_PROVIDER
                        )
                }
            }
        }
        ContextCompat.registerReceiver(
            currentContext, gpsReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose { currentContext.unregisterReceiver(gpsReceiver) }
    }

    LaunchedEffect(isGpsActive.value) {
        if (isGpsActive.value) {
            val hasFinePermission = ContextCompat.checkSelfPermission(
                currentContext, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (hasFinePermission) {
                locationHelper.addLocationUpdateListener { location ->
                    latitude.doubleValue = location.latitude
                    longitude.doubleValue = location.longitude
                }
            }
        }
    }

    LaunchedEffect(isMapCentered, latitude.doubleValue, longitude.doubleValue) {
        if (isMapCentered) {
            val position = Position(
                latitude = latitude.doubleValue, longitude = longitude.doubleValue
            )
            mapState.animateCameraPosition(
                position = mapState.cameraPosition.copy(target = position, zoom = 13.0),
                duration = 2.seconds
            )
        }
    }

    LaunchedEffect(friend.value) {
        val currentFriend = friend.value
        if (currentFriend != null) {
            val position = Position(
                latitude = currentFriend.latitude, longitude = currentFriend.longitude
            )
            mapState.animateCameraPosition(
                position = mapState.cameraPosition.copy(target = position, zoom = 13.0),
                duration = 2.seconds
            )

            isMapCentered = false
            friend.value = null
        }
    }

    LaunchedEffect(mapState) {
        snapshotFlow { mapState.cameraMoveReason }.collectLatest { reason ->
            if (reason == CameraMoveReason.GESTURE) {
                isMapCentered = false
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        MaplibreMap(state = mapState)

        FloatingActionButton(
            onClick = {
                if (latitude.doubleValue != 0.0 && longitude.doubleValue != 0.0) isMapCentered =
                    true
            }, modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                painter = if (isMapCentered) painterResource(id = R.drawable.ic_my_location)
                else painterResource(id = R.drawable.ic_my_location_search),
                contentDescription = "Мое местоположение"
            )
        }
    }
}

@Composable
fun Friends(
    modifier: Modifier = Modifier,
    friends: SnapshotStateList<User>,
    friend: MutableState<User?>,
    onNavigateToMap: () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        val errorMessage = stringResource(R.string.failed_to_accept_the_friend_request)
        val errorMessage2 = stringResource(R.string.failed_to_reject_the_friend_request)

        var showSendDialog by remember { mutableStateOf(false) }
        var removableFriend by remember { mutableStateOf<User?>(null) }
        val snackBarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

        var userName by remember { mutableStateOf("") }

        LazyColumn {
            items(friends) { user ->
                User(user, onClick = {
                    friend.value = user
                    onNavigateToMap()
                }, onLongClick = {
                    removableFriend = user
                }, onAcceptClick = {
                    val database = Firebase.database
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
                    val database = Firebase.database
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
            onClick = {
                showSendDialog = true
            }, modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_add),
                contentDescription = "Добавить кента"
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
                            val database = Firebase.database
                            val searchRef = database.getReference("users/${editUserName}").get()

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
                        val database = Firebase.database
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
            .combinedClickable(onClick = { onClick() }, onLongClick = { onLongClick() })
            .padding(16.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_account_box),
            contentDescription = stringResource(id = R.string.my_photo),
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .border(
                    border = BorderStroke(
                        width = 2.dp, color = MaterialTheme.colorScheme.primary
                    ), shape = CircleShape
                ),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(user.name, style = MaterialTheme.typography.titleLarge)
            Text(user.userName, style = MaterialTheme.typography.titleMedium)
            Text(
                text = when (user.status) {
                    0 -> stringResource(R.string.offline)
                    1 -> stringResource(R.string.sharing_location)
                    2 -> stringResource(R.string.online)
                    else -> stringResource(R.string.offline)
                }, style = MaterialTheme.typography.titleSmall
            )
        }

        if (!user.isFriend) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismissClick) {
                    Icon(
                        painterResource(R.drawable.ic_close),
                        contentDescription = "Отклонить",
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
                        contentDescription = "Принять",
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
    oldName: MutableState<String>
) {
    val snackBarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Image(
                painter = painterResource(id = R.drawable.ic_account_box),
                contentDescription = stringResource(id = R.string.my_photo),
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .border(
                        border = BorderStroke(3.dp, MaterialTheme.colorScheme.primary),
                        shape = CircleShape
                    ),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(16.dp))

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

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = name.value,
                onValueChange = { name.value = it },
                label = { Text(stringResource(R.string.name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val newUserName = userName.value.replace("@", "").trim()

                    val usersRef = database.getReference("users")
                    val newUserNameRef = usersRef.child(newUserName).get()
                    val oldUserNameRef = usersRef.child(oldUserName.value)

                    if (userName.value != oldUserName.value) {
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
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = userName.value != loadingStr && name.value != loadingStr && (userName.value != oldUserName.value || name.value != oldName.value)
            ) {
                Text(stringResource(R.string.save_changes))
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    Firebase.auth.signOut()
                    currentUser = null
                }, modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.sign_out))
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
fun Register(modifier: Modifier = Modifier) {
    val snackBarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(id = R.string.email)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Spacer(modifier = Modifier.height(8.dp))

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

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password1,
                onValueChange = { password1 = it },
                label = { Text(stringResource(id = R.string.password)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password2,
                onValueChange = { password2 = it },
                label = { Text(stringResource(R.string.repeat_password)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (password1 == password2) {
                        val newUserName = userName.replace("@", "").trim()

                        val database = Firebase.database
                        val auth = Firebase.auth

                        val checkUserName = database.getReference("users/$newUserName").get()
                        checkUserName.addOnSuccessListener { checkSnap ->
                            if (checkSnap.value == null) {
                                val createUser =
                                    auth.createUserWithEmailAndPassword(email, password1)
                                createUser.addOnSuccessListener {
                                    currentUser = it.user
                                    val uid = currentUser!!.uid
                                    val reference = database.getReference(uid)
                                    val uidRef = database.getReference("users/$newUserName/uid")
                                    reference.child("userName").setValue(newUserName)
                                    reference.child("name").setValue(name)
                                    uidRef.setValue(uid)
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
                modifier = Modifier.fillMaxWidth(),
                enabled = email.isNotEmpty() && userName.isNotEmpty() && password1.isNotEmpty() && password2.isNotEmpty()
            ) {
                Text(stringResource(id = R.string.register))
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    currentAuthScreen = "sign_in"
                }, modifier = Modifier.fillMaxWidth()
            ) {
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

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(id = R.string.email)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(id = R.string.password)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
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
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = email.isNotEmpty() && password.isNotEmpty()
            ) {
                Text(stringResource(id = R.string.sign_in))
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    currentAuthScreen = "register"
                }, modifier = Modifier.fillMaxWidth()
            ) {
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
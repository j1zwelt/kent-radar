package ru.j1zwelt.kentradar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.database
import kotlinx.coroutines.launch
import ru.j1zwelt.kentradar.ui.theme.KentRadarTheme

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
}

@PreviewScreenSizes
@Composable
fun KentRadarApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.MAP) }

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
                    Map(modifier = Modifier.padding(innerPadding))
                }

                AppDestinations.FRIENDS -> {
                    Friends(modifier = Modifier.padding(innerPadding))
                }

                AppDestinations.PROFILE -> {
                    Profile(modifier = Modifier.padding(innerPadding))
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
fun Map(modifier: Modifier = Modifier) {
    Text("Map", modifier = modifier)
}

@Composable
fun Friends(modifier: Modifier = Modifier) {
    Text("Friends", modifier = modifier)
}

@Composable
fun Profile(modifier: Modifier = Modifier) {
    val loadingStr = stringResource(R.string.loading)

    var userName by remember { mutableStateOf(loadingStr) }
    var name by remember { mutableStateOf(loadingStr) }

    val database = Firebase.database
    val userNameRef = database.getReference("${currentUser!!.uid}/userName")
    val nameRef = database.getReference("${currentUser!!.uid}/name")

    LaunchedEffect(key1 = Unit) {
        userNameRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) userName = snapshot.value.toString()
        }

        nameRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) name = snapshot.value.toString()
        }
    }

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
            value = userName,
            onValueChange = { userName = it },
            label = { Text(stringResource(R.string.username)) },
            leadingIcon = {
                Text(
                    text = "@",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.name)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                userNameRef.setValue(userName)
                nameRef.setValue(name)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = userName != loadingStr && name != loadingStr
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
}

@Composable
fun Register(modifier: Modifier = Modifier) {
    val snackBarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val errorMessage = stringResource(R.string.unknown_error)
    val errorMessage2 = stringResource(R.string.passwords_do_not_match)

    var email by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var password1 by remember { mutableStateOf("") }
    var password2 by remember { mutableStateOf("") }

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
                onValueChange = { userName = it },
                label = { Text(stringResource(R.string.username)) },
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
                        val auth = Firebase.auth.createUserWithEmailAndPassword(email, password1)
                        auth.addOnSuccessListener {
                            currentUser = it.user
                            val uid = currentUser!!.uid
                            val database = Firebase.database
                            val userNameRef = database.getReference("$uid/userName")
                            val nameRef = database.getReference("$uid/name")
                            val uidRef = database.getReference("$userName/uid")
                            userNameRef.setValue(userName.replace("@", ""))
                            nameRef.setValue(name)
                            uidRef.setValue(uid)
                        }
                        auth.addOnFailureListener {
                            scope.launch {
                                snackBarHostState.showSnackbar(
                                    message = it.localizedMessage ?: errorMessage
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
                    val auth = Firebase.auth.signInWithEmailAndPassword(email, password)
                    auth.addOnSuccessListener {
                        currentUser = it.user
                    }
                    auth.addOnFailureListener {
                        scope.launch {
                            snackBarHostState.showSnackbar(
                                message = it.localizedMessage ?: errorMessage
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
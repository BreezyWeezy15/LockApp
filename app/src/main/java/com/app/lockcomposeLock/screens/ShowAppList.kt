import android.app.Application
import android.content.Intent
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Base64
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.lockcomposeLock.services.AppLockService
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


@Composable
fun ShowAppList() {
    val context = LocalContext.current.applicationContext as Application
    val selectedApps = remember { mutableStateListOf<InstalledApps>() }
    val isLoading = remember { mutableStateOf(true) }


    LaunchedEffect(Unit) {
        val serviceIntent = Intent(context, AppLockService::class.java)
        context.startService(serviceIntent)
    }

    // Fetch apps from Firebase when the Composable is first launched
    LaunchedEffect(Unit) {
        fetchAppsFromFirebase { apps ->
            selectedApps.clear()
            selectedApps.addAll(apps)
            isLoading.value = false
        }
    }

    if (isLoading.value) {
        // Show loading indicator
        CircularProgressIndicator()
    } else {
        LazyColumn {
            items(selectedApps) { app ->
                AppListItem(
                    app = app,
                    interval = app.interval,
                    pinCode = app.pinCode
                )
            }
        }
    }
}

@Composable
fun AppListItem(app: InstalledApps, interval: String, pinCode: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(12.dp) // Rounded card corners
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    bitmap = app.icon!!.toBitmap().asImageBitmap(),
                    contentDescription = app.name,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape) // Make the icon circular
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) // Add a border
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Interval and Pin Code Section
            Column(
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = "Interval: $interval Min",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Pin Code: $pinCode",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

data class InstalledApps(
    val packageName: String,
    val name: String,
    val icon: Drawable?,
    val interval: String,
    val pinCode: String
)

private fun fetchAppsFromFirebase(onAppsFetched: (List<InstalledApps>) -> Unit) {
    val database = FirebaseDatabase.getInstance().reference
    val appsListRef = database.child("childApp")

    appsListRef.addValueEventListener(object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            val updatedList = mutableListOf<InstalledApps>()

            for (childSnapshot in dataSnapshot.children) {
                val packageName = childSnapshot.child("package_name").getValue(String::class.java) ?: ""
                val name = childSnapshot.child("name").getValue(String::class.java) ?: ""
                val base64Icon = childSnapshot.child("icon").getValue(String::class.java) ?: ""
                val interval = childSnapshot.child("interval").getValue(String::class.java) ?: ""
                val pinCode = childSnapshot.child("pin_code").getValue(String::class.java) ?: ""

                // Convert Base64 string to Bitmap
                val iconBitmap = base64ToBitmap(base64Icon)

                val installedApp = InstalledApps(
                    packageName = packageName,
                    name = name,
                    icon = iconBitmap,
                    interval = interval,
                    pinCode = pinCode
                )
                updatedList.add(installedApp)
            }
            onAppsFetched(updatedList) // Callback to update the UI
        }

        override fun onCancelled(databaseError: DatabaseError) {
            Log.e("FirebaseError", "Error fetching data: ${databaseError.message}")
            // Optionally, handle the error or update the UI to show an error message
        }
    })
}

// Function to convert Base64 string to Bitmap
fun base64ToBitmap(base64String: String): Drawable? {
    return try {
        val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)?.let {
            BitmapDrawable(Resources.getSystem(), it)
        }
    } catch (e: Exception) {
        null
    }
}
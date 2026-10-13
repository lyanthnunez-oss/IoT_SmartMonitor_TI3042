package cl.ti3042.smartmonitor

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private val db by lazy { FirebaseDatabase.getInstance().reference }
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var temp: EditText
    private lateinit var humidity: EditText
    private lateinit var status: TextView
    private lateinit var tempText: TextView
    private lateinit var humidityText: TextView
    private lateinit var deviceState: TextView
    private var readingListener: ValueEventListener? = null
    private var deviceListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        auth = FirebaseAuth.getInstance()
        email = findViewById(R.id.emailInput)
        password = findViewById(R.id.passwordInput)
        temp = findViewById(R.id.tempInput)
        humidity = findViewById(R.id.humidityInput)
        status = findViewById(R.id.statusText)
        tempText = findViewById(R.id.tempText)
        humidityText = findViewById(R.id.humidityText)
        deviceState = findViewById(R.id.deviceStateText)

        findViewById<Button>(R.id.loginButton).setOnClickListener { login() }
        findViewById<Button>(R.id.registerButton).setOnClickListener { register() }
        findViewById<Button>(R.id.sendReadingButton).setOnClickListener { publishReading() }
        findViewById<Button>(R.id.deviceButton).setOnClickListener { toggleDevice() }
        findViewById<Button>(R.id.logoutButton).setOnClickListener {
            auth.signOut()
            detachListeners()
            status.text = "Estado: sesión cerrada"
            toast("Sesión cerrada")
        }
        auth.currentUser?.let { startMonitoring() }
    }

    private fun login() {
        val e = email.text.toString().trim()
        val p = password.text.toString()
        if (e.isBlank() || p.length < 6) { toast("Ingresa correo y contraseña (mínimo 6 caracteres)"); return }
        auth.signInWithEmailAndPassword(e, p).addOnCompleteListener { task ->
            if (task.isSuccessful) startMonitoring()
            else toast("No se pudo iniciar sesión. Verifica las credenciales y Firebase.")
        }
    }

    private fun register() {
        val e = email.text.toString().trim()
        val p = password.text.toString()
        if (e.isBlank() || p.length < 6) { toast("Ingresa correo y contraseña (mínimo 6 caracteres)"); return }
        auth.createUserWithEmailAndPassword(e, p).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val uid = auth.currentUser?.uid ?: return@addOnCompleteListener
                db.child("users").child(uid).child("email").setValue(e)
                startMonitoring()
            } else toast("No se pudo crear la cuenta. Revisa la configuración de Firebase.")
        }
    }

    private fun startMonitoring() {
        status.text = "Conectado: ${auth.currentUser?.email}"
        detachListeners()
        readingListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val t = snapshot.child("temperature").getValue(Double::class.java)
                val h = snapshot.child("humidity").getValue(Double::class.java)
                if (t != null) tempText.text = "Temperatura: %.1f °C".format(t)
                if (h != null) humidityText.text = "Humedad: %.1f %%".format(h)
            }
            override fun onCancelled(error: DatabaseError) { toast("Lectura denegada o sin conexión") }
        }
        db.child("latestReading").addValueEventListener(readingListener!!)
        deviceListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val on = snapshot.getValue(Boolean::class.java) ?: false
                deviceState.text = if (on) "Dispositivo: encendido" else "Dispositivo: apagado"
            }
            override fun onCancelled(error: DatabaseError) { toast("No se pudo leer el estado del dispositivo") }
        }
        db.child("device").child("isOn").addValueEventListener(deviceListener!!)
    }

    private fun publishReading() {
        if (auth.currentUser == null) { toast("Inicia sesión primero"); return }
        val t = temp.text.toString().toDoubleOrNull()
        val h = humidity.text.toString().toDoubleOrNull()
        if (t == null || h == null || h !in 0.0..100.0 || t !in -40.0..125.0) {
            toast("Valores inválidos: temperatura -40 a 125 °C; humedad 0 a 100 %"); return
        }
        val data = mapOf(
            "temperature" to t, "humidity" to h,
            "timestamp" to System.currentTimeMillis(),
            "userId" to auth.currentUser!!.uid
        )
        db.child("latestReading").setValue(data)
            .addOnSuccessListener { toast("Medición compartida con los dispositivos conectados") }
            .addOnFailureListener { toast("Error al guardar: revisa las reglas y la conexión") }
    }

    private fun toggleDevice() {
        if (auth.currentUser == null) { toast("Inicia sesión primero"); return }
        db.child("device").child("isOn").get().addOnSuccessListener { snap ->
            val current = snap.getValue(Boolean::class.java) ?: false
            db.child("device").child("isOn").setValue(!current)
                .addOnFailureListener { toast("No se pudo actualizar el dispositivo") }
        }
    }

    private fun detachListeners() {
        readingListener?.let { db.child("latestReading").removeEventListener(it) }
        deviceListener?.let { db.child("device").child("isOn").removeEventListener(it) }
        readingListener = null
        deviceListener = null
    }
    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    override fun onDestroy() { detachListeners(); super.onDestroy() }
}

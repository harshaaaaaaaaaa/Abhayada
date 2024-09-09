import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.help.Abhayada.MainActivity

class AppInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_PACKAGE_ADDED) {
            val packageName = intent.data?.encodedSchemeSpecificPart
            if (packageName == context.packageName) {
                val mainActivityIntent = Intent(context, MainActivity::class.java)
                mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(mainActivityIntent)
            }
        }
    }
}
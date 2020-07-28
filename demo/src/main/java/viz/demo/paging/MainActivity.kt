package viz.demo.paging

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import viz.demo.paging.ui.main.MainFragment
import viz.demo.paging.ui.main.Paging3Fragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.container, Paging3Fragment.newInstance())
                    .commitNow()
        }
    }
}
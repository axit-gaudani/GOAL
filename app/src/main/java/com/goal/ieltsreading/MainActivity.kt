package com.goal.ieltsreading

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.goal.ieltsreading.databinding.ActivityMainBinding
import com.goal.ieltsreading.ui.common.visibleIf
import com.goal.ieltsreading.ui.welcome.WelcomeActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val topLevelScreens = setOf(
        R.id.homeFragment, R.id.levelsFragment, R.id.vocabHubFragment, R.id.speakHubFragment, R.id.progressFragment
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as IeltsApp
        if (!app.prefs.onboarded) {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        val navController = navHost.navController
        setupActionBarWithNavController(navController, AppBarConfiguration(topLevelScreens))
        binding.bottomNav.setupWithNavController(navController)

        // Hide the bottom bar on focused learning screens (lesson, quiz, test...).
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNav.visibleIf(destination.id in topLevelScreens)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        return navHost.navController.navigateUp() || super.onSupportNavigateUp()
    }
}

package com.xmm.videodownloader

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.xmm.videodownloader.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.getSelectedColor(this).let {
            setTheme(resources.getIdentifier("AppTheme_${it.name}", "style", packageName))
        }
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            loadFragment(ParseFragment())
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_parse -> { loadFragment(ParseFragment()); true }
                R.id.nav_downloads -> { loadFragment(DownloadsFragment()); true }
                R.id.nav_local -> { loadFragment(LocalVideosFragment()); true }
                R.id.nav_settings -> { loadFragment(SettingsFragment()); true }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    fun switchTheme(index: Int) {
        ThemeManager.setSelectedIndex(this, index)
        recreate()
    }

    fun switchLanguage(code: String) {
        LanguageManager.setSelectedCode(this, code)
        recreate()
    }
}

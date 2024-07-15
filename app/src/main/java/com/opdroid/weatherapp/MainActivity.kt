package com.opdroid.weatherapp

import android.Manifest
import java.text.SimpleDateFormat
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.opdroid.weatherapp.models.WeatherResponse
import com.opdroid.weatherapp.network.WeatherService
import com.opdroid.weatherapp.utils.Constants
import retrofit.GsonConverterFactory
import retrofit.*
import java.util.Date
import java.util.TimeZone

// OpenWeather Link : https://openweathermap.org/api
//myApi = 1fabca3d20aba6d425d058e008636c44
/**
 * The useful link or some more explanation for this app you can checkout this link :
 * https://medium.com/@sasude9/basic-android-weather-app-6a7c0855caf4
 */
class MainActivity : AppCompatActivity() {
    // A fused location client variable which is further user to get the user's current location
    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    private lateinit var tv_main: TextView
    private lateinit var tv_main_description: TextView
    private lateinit var tv_temp: TextView
    private lateinit var tv_humidity: TextView
    private lateinit var tv_min: TextView
    private lateinit var tv_max: TextView
    private lateinit var tv_speed: TextView
    private lateinit var tv_name: TextView
    private lateinit var tv_country: TextView
    private lateinit var tv_sunrise_time: TextView
    private lateinit var tv_sunset_time: TextView
    private lateinit var iv_main: ImageView



    // A global variable for Progress Dialog
    private var mProgressDialog: Dialog? = null

    // A global variable for Current Latitude
    private var mLatitude: Double = 0.0
    // A global variable for Current Longitude
    private var mLongitude: Double = 0.0

    // TODO (STEP 1: Add a variable for SharedPreferences)
    // START
    // A global variable for the SharedPreferences
    private lateinit var mSharedPreferences: SharedPreferences
    // END

    // for ads
    private var mInterstitialAd: InterstitialAd? = null
    private final val TAG = "MainActivity"
    private var rewardedAd: RewardedAd? = null
    lateinit var showInterstitialAd: Button
    lateinit var showRewardAd: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        MobileAds.initialize(this) {} //for ads
        loadBanner()
        loadNativeAd()

        // interstitial ad

        showInterstitialAd= findViewById(R.id.showInterstial)
        showInterstitialAd.setOnClickListener {
            loadInterstitial()
//            mInterstitialAd?.show(this)
        }

        // reward ads
        showRewardAd= findViewById(R.id.reward)
        showRewardAd.setOnClickListener {
            loadReward()
        }

        //end

        tv_main = findViewById(R.id.tv_main)
        tv_main_description = findViewById(R.id.tv_main_description)
        tv_temp = findViewById(R.id.tv_temp)
        tv_humidity = findViewById(R.id.tv_humidity)
        tv_min = findViewById(R.id.tv_min)
        tv_max = findViewById(R.id.tv_max)
        tv_speed = findViewById(R.id.tv_speed)
        tv_name = findViewById(R.id.tv_name)
        tv_country = findViewById(R.id.tv_country)
        tv_sunrise_time = findViewById(R.id.tv_sunrise_time)
        tv_sunset_time = findViewById(R.id.tv_sunset_time)
        iv_main = findViewById(R.id.iv_main)

        // Initialize the Fused location variable
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // TODO (STEP 2: Initialize the SharedPreferences variable.)
        // START
        // Initialize the SharedPreferences variable
        mSharedPreferences = getSharedPreferences(Constants.PREFERENCE_NAME, MODE_PRIVATE)
        // END

        // TODO (STEP 7: Call the UI method to populate the data in
        //  the UI which are already stored in sharedPreferences earlier.
        //  At first run it will be blank.)
        // START
        setupUI()
        // END


        if (!isLocationEnabled()) {
            Toast.makeText(this, "Please enable location services", Toast.LENGTH_SHORT).show()

            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent) //automatically goes to settings to enable location services
        }
        else {
//            Toast.makeText(this, "Location services is already enabled", Toast.LENGTH_SHORT).show()
            // TODO (STEP 1: Asking the location permission on runtime.)
            // START
            Dexter.withActivity(this)
                .withPermissions(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        if (report!!.areAllPermissionsGranted()) {
                            // TODO (STEP 7: Call the location request function here.)
                            // START
                            requestLocationData()
                            // END
                        }

                        if (report.isAnyPermissionPermanentlyDenied) {
                            Toast.makeText(
                                this@MainActivity,
                                "You have denied location permission. Please allow, it is mandatory.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<PermissionRequest>?,
                        token: PermissionToken?
                    ) {
                        showRationalDialogForPermissions()
                    }
                }).onSameThread()
                .check()
            // END
        }


//        CallAPILoginAsyncTask("op","123456789").execute()
        }

    //adding ad

    // Determine the screen width (less decorations) to use for the ad width.
// If the ad hasn't been laid out, default to the full screen width.

    private fun loadBanner() {

        // Create a new ad view.
        val adView = AdView(this)
//        adView.adSizes = adSize
        adView.setAdSize(AdSize.BANNER)
        adView.adUnitId = "ca-app-pub-3940256099942544/9214589741"

        // Create an ad request.
        val adRequest = AdRequest.Builder().build()

        // Start loading the ad in the background.
        adView.loadAd(adRequest)
        val layout = findViewById<LinearLayout>(R.id.adContainer) // replace with your layout ID
layout.addView(adView)

        adView.adListener = object: AdListener() {
            override fun onAdClicked() {
                // Code to be executed when the user clicks on an ad.
            }

            override fun onAdClosed() {
                // Code to be executed when the user is about to return
                // to the app after tapping on an ad.
            }

            override fun onAdFailedToLoad(adError : LoadAdError) {
                // Code to be executed when an ad request fails.
            }

            override fun onAdImpression() {
                // Code to be executed when an impression is recorded
                // for an ad.
            }

            override fun onAdLoaded() {
                // Code to be executed when an ad finishes loading.
            }

            override fun onAdOpened() {
                // Code to be executed when an ad opens an overlay that
                // covers the screen.
            }
        }
    }

    private fun loadNativeAd(){
        val adLoader = AdLoader.Builder(this, "ca-app-pub-3940256099942544/2247696110")
            .forNativeAd { ad : NativeAd ->
                // Show the ad.
                if (isDestroyed) {
                    ad.destroy()
                    return@forNativeAd
                }
            }
            .withAdListener(object : AdListener() {
                // AdListener callbacks can be overridden here.
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    // Handle the failure by logging, altering the UI, and so on.
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                // Methods in the NativeAdOptions.Builder class can be
                // used here to specify individual options settings.
                .build())
            .build()

        adLoader.loadAd(AdRequest.Builder().build())

        if (adLoader.isLoading) {
            // The AdLoader is still loading ads.
            // Expect more adLoaded or onAdFailedToLoad callbacks.
        } else {
            // The AdLoader has finished loading ads.
        }
    }

    private fun loadInterstitial(){
        var adRequest = AdRequest.Builder().build()

        InterstitialAd.load(this,"ca-app-pub-3940256099942544/1033173712", adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                adError?.toString()?.let { Log.d(TAG, it) }
                mInterstitialAd = null
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                Log.d(TAG, "Ad was loaded")
                mInterstitialAd = interstitialAd
            }
        })

        mInterstitialAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
            override fun onAdClicked() {
                // Called when a click is recorded for an ad.
                Log.d(TAG, "Ad was clicked.")
            }

            override fun onAdDismissedFullScreenContent() {
                // Called when ad is dismissed.
                Log.d(TAG, "Ad dismissed fullscreen content.")
                mInterstitialAd = null
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                // Called when ad fails to show.
                Log.e(TAG, "Ad failed to show fullscreen content.")
                mInterstitialAd = null
            }

            override fun onAdImpression() {
                // Called when an impression is recorded for an ad.
                Log.d(TAG, "Ad recorded an impression.")
            }

            override fun onAdShowedFullScreenContent() {
                // Called when ad is shown.
                Log.d(TAG, "Ad showed fullscreen content.")
            }
        }
        mInterstitialAd?.show(this)
    }

    private fun loadReward(){
        var adRequest = AdRequest.Builder().build()
        RewardedAd.load(this,"ca-app-pub-3940256099942544/5224354917", adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                adError?.toString()?.let { Log.d(TAG, it) }
                rewardedAd = null
            }

            override fun onAdLoaded(ad: RewardedAd) {
                Log.d(TAG, "Ad was loaded.")
                rewardedAd = ad
            }
        })

        rewardedAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
            override fun onAdClicked() {
                // Called when a click is recorded for an ad.
                Log.d(TAG, "Ad was clicked.")
            }

            override fun onAdDismissedFullScreenContent() {
                // Called when ad is dismissed.
                // Set the ad reference to null so you don't show the ad a second time.
                Log.d(TAG, "Ad dismissed fullscreen content.")
                rewardedAd = null
            }

            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                // Called when ad fails to show.
                Log.e(TAG, "Ad failed to show fullscreen content.")
                rewardedAd = null
            }

            override fun onAdImpression() {
                // Called when an impression is recorded for an ad.
                Log.d(TAG, "Ad recorded an impression.")
            }

            override fun onAdShowedFullScreenContent() {
                // Called when ad is shown.
                Log.d(TAG, "Ad showed fullscreen content.")
            }
        }

        rewardedAd?.let { ad ->
            ad.show(this, OnUserEarnedRewardListener { rewardItem ->
                // Handle the reward.
                val rewardAmount = rewardItem.amount
                val rewardType = rewardItem.type
                Log.d(TAG, "User earned the reward.")
            })
        } ?: run {
            Log.d(TAG, "The rewarded ad wasn't ready yet.")
        }
    }
    //

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true // return true to display the menu
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                getLocationWeatherDetails(mLatitude,mLongitude)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager:LocationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    // TODO (STEP 2: A alert dialog for denied permissions and if needed to allow it from the settings app info.)
    // START
    /**
     * A function used to show the alert dialog when the permissions are denied and need to allow it from settings app info.
     */
    private fun showRationalDialogForPermissions() {
        AlertDialog.Builder(this)
            .setMessage("It Looks like you have turned off permissions required for this feature. It can be enabled under Application Settings")
            .setPositiveButton(
                "GO TO SETTINGS"
            ) { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel") { dialog,
                                           _ ->
                dialog.dismiss()
            }.show()
    }
    // END

    // TODO (STEP 5: Add a function to get the location of the device using the fusedLocationProviderClient.)
    // START
    /**
     * A function to request the current location. Using the fused location provider client.
     */
    @SuppressLint("MissingPermission")
    private fun requestLocationData() {

        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()
        )
    }
    // END

    // TODO (STEP 6: Register a request location callback to get the location.)
    // START
    /**
     * A location callback object of fused location provider client where we will get the current location details.
     */
    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location? = locationResult.lastLocation
            val latitude = mLastLocation?.latitude
            Log.i("Current Latitude", "$latitude")

            val longitude = mLastLocation?.longitude
            Log.i("Current Longitude", "$longitude")
            getLocationWeatherDetails(latitude!!, longitude!!) // used !! to avoid null pointer exception
        }
    }
    // END

    private fun getLocationWeatherDetails(latitude: Double, longitude: Double) {

        if (Constants.isNetworkAvailable(this@MainActivity)) {

            /**
             * Add the built-in converter factory first. This prevents overriding its
             * behavior but also ensures correct behavior when using converters that consume all types.
             */
            val retrofit: Retrofit = Retrofit.Builder()
                // API base URL.
                .baseUrl(Constants.BASE_URL)
                /** Add converter factory for serialization and deserialization of objects. */
                /**
                 * Create an instance using a default {@link Gson} instance for conversion. Encoding to JSON and
                 * decoding from JSON (when no charset is specified by a header) will use UTF-8.
                 */
                .addConverterFactory(GsonConverterFactory.create())
                /** Create the Retrofit instances. */

                .build()

            /**
             * Here we map the service interface in which we declares the end point and the API type
             *i.e GET, POST and so on along with the request parameter which are required.
             */
            val service: WeatherService =
                retrofit.create<WeatherService>(WeatherService::class.java)

            /** An invocation of a Retrofit method that sends a request to a web-server and returns a response.
             * Here we pass the required param in the service
             */
            val listCall: Call<WeatherResponse> = service.getWeather(
                latitude, longitude, Constants.METRIC_UNIT, Constants.APP_ID
            )

            // TODO (STEP 6: Show the progress dialog)
            // START
            showCustomProgressDialog() // Used to show the progress dialog
            // END

            // Callback methods are executed using the Retrofit callback executor.
            listCall.enqueue(object : Callback<WeatherResponse> {
                @SuppressLint("SetTextI18n")
                override fun onResponse(
                    response: Response<WeatherResponse>,
                    retrofit: Retrofit
                ) {

                    // Check weather the response is success or not.
                    if (response.isSuccess) {

                        // TODO (STEP 7: Hide the progress dialog)
                        // START
                        hideProgressDialog() // Hides the progress dialog
                        // END

                        /** The de-serialized response body of a successful response. */
                        val weatherList: WeatherResponse = response.body()
                        Log.i("Response Result", "$weatherList")

                        // TODO (STEP 4: Here we convert the response object to string and store the string in the SharedPreference.)
                        // START
                        // Here we have converted the model class in to Json String to store it in the SharedPreferences.
                        val weatherResponseJsonString = Gson().toJson(weatherList)
                        // Save the converted string to shared preferences
                        val editor = mSharedPreferences.edit()
                        editor.putString(Constants.WEATHER_RESPONSE_DATA, weatherResponseJsonString)
                        editor.apply()
                        // END

                        // TODO (STEP 5: Remove the weather detail object as we will be getting
                        //  the object in form of a string in the setup UI method.)
                        // START
                        setupUI()
                        // END
                    } else {
                        // If the response is not "success" then we check the response code.
                        val rc = response.code()
                        when (rc) {
                            400 -> {
                                Log.e("Error 400", "Bad Request")
                            }
                            404 -> {
                                Log.e("Error 404", "Not Found")
                            }
                            else -> {
                                Log.e("Error", "Generic Error $rc")
                            }
                        }
                    }
                }

                override fun onFailure(t: Throwable) {
                    // TODO (STEP 8: Hide the progress dialog)
                    // START
                    hideProgressDialog() // Hides the progress dialog
                    // END
                    Log.e("Errorrrrr", t.message.toString())
                }
            })
        } else {
            Toast.makeText(
                this@MainActivity,
                "No internet connection available.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // TODO (STEP 5: Create a functions for SHOW and HIDE progress dialog.)
    // START
    /**
     * Method is used to show the Custom Progress Dialog.
     */
    private fun showCustomProgressDialog() {
        mProgressDialog = Dialog(this)

        /*Set the screen content from a layout resource.
        The resource will be inflated, adding all top-level views to the screen.*/
        mProgressDialog!!.setContentView(R.layout.dialog_custom_progress)

        //Start the dialog and display it on screen.
        mProgressDialog!!.show()
    }

    /**
     * This function is used to dismiss the progress dialog if it is visible to user.
     */
    private fun hideProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog!!.dismiss()
        }
    }
    // END
    /**
     * Function is used to set the result in the UI elements.
     */
    private fun setupUI() {
        // TODO (STEP 6: Here we get the stored response from
        //  SharedPreferences and again convert back to data object
        //  to populate the data in the UI.)
        // START
        // Here we have got the latest stored response from the SharedPreference and converted back to the data model object.
        val weatherResponseJsonString =
            mSharedPreferences.getString(Constants.WEATHER_RESPONSE_DATA, "")

        if (!weatherResponseJsonString.isNullOrEmpty()) {

            val weatherList =
                Gson().fromJson(weatherResponseJsonString, WeatherResponse::class.java)

            // For loop to get the required data. And all are populated in the UI.
            for (z in weatherList.weather.indices) {
                Log.i("NAMEEEEEEEE", weatherList.weather[z].main)

                tv_main.text = weatherList.weather[z].main
                tv_main_description.text = weatherList.weather[z].description
                tv_temp.text =
                    weatherList.main.temp.toString() + getUnit(application.resources.configuration.locales.toString())
                tv_humidity.text = weatherList.main.humidity.toString() + " per cent"
                tv_min.text = weatherList.main.tempMin.toString() + " min"
                tv_max.text = weatherList.main.tempMax.toString() + " max"
                tv_speed.text = weatherList.wind.speed.toString()
                tv_name.text = weatherList.name
                tv_country.text = weatherList.sys.country
                tv_sunrise_time.text = unixTime(weatherList.sys.sunrise.toLong())
                tv_sunset_time.text = unixTime(weatherList.sys.sunset.toLong())

                // Here we update the main icon
                when (weatherList.weather[z].icon) {
                    "01d" -> iv_main.setImageResource(R.drawable.sunny)
                    "02d" -> iv_main.setImageResource(R.drawable.cloud)
                    "03d" -> iv_main.setImageResource(R.drawable.cloud)
                    "04d" -> iv_main.setImageResource(R.drawable.cloud)
                    "04n" -> iv_main.setImageResource(R.drawable.cloud)
                    "10d" -> iv_main.setImageResource(R.drawable.rain)
                    "11d" -> iv_main.setImageResource(R.drawable.storm)
                    "13d" -> iv_main.setImageResource(R.drawable.snowflake)
                    "01n" -> iv_main.setImageResource(R.drawable.cloud)
                    "02n" -> iv_main.setImageResource(R.drawable.cloud)
                    "03n" -> iv_main.setImageResource(R.drawable.cloud)
                    "10n" -> iv_main.setImageResource(R.drawable.cloud)
                    "11n" -> iv_main.setImageResource(R.drawable.rain)
                    "13n" -> iv_main.setImageResource(R.drawable.snowflake)
                }
            }
        }
        // END
    }

    /**
     * Function is used to get the temperature unit value.
     */
    private fun getUnit(value: String): String? {
        Log.i("unitttttt", value)
        var value = "°C"
        if ("US" == value || "LR" == value || "MM" == value) {
            value = "°F"
        }
        return value
    }

    /**
     * The function is used to get the formatted time based on the Format and the LOCALE we pass to it.
     */
    private fun unixTime(timex: Long): String? {
        val date = Date(timex * 1000L)
        @SuppressLint("SimpleDateFormat") val sdf =
            SimpleDateFormat("HH:mm:ss")
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(date)
    }
}










































    /*
    private inner class CallAPILoginAsyncTask(val userName:String,val password:String): AsyncTask<Any, Void, String>() {
        private lateinit var customProgressDialog: Dialog

        override fun onPreExecute() {
            super.onPreExecute()
            showProgressDialog()
        }

        override fun doInBackground(vararg params: Any?): String {
            var result: String
            var connection: HttpURLConnection? = null

            try {
                val url = URL("https://run.mocky.io/v3/4200a2e0-74e2-4ab4-873d-4eaa05e21fb4")
                connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.doOutput = true

                connection.instanceFollowRedirects = false
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("charset", "utf-8")
                connection.setRequestProperty("Accept", "application/json")

                connection.useCaches = false

                val writeDataOutputStream = DataOutputStream(connection.outputStream)
                val jsonRequest = JSONObject()
                jsonRequest.put("username", userName)
                jsonRequest.put("password", password)

                writeDataOutputStream.writeBytes(jsonRequest.toString())
                writeDataOutputStream.flush()
                writeDataOutputStream.close()

                val httpResult: Int = connection.responseCode

                if (httpResult == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val stringBuilder = StringBuilder()
                    var line: String?
                    try {
                        while (reader.readLine().also { line = it } != null) {
                            stringBuilder.append(line + "\n")
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                    finally {
                        try {
                            inputStream.close()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                    result = stringBuilder.toString()
                } else {
                    result = connection.responseMessage
                }
            }
            catch (e: SocketTimeoutException) {
                result = "Connection Timeout"
            }
            catch (e: Exception) {
                result = "Error: ${e.message}"
            }
            finally {
                connection?.disconnect()
            }
            return result
        }

        override fun onPostExecute(result: String?){
            super.onPostExecute(result)
            cancelProgressDialog()

            Log.i("JSON Response Result", result.toString())

            // Map the json response with the Data Class using GSON.
            val responseData = Gson().fromJson(result, ResponseData::class.java)

            Log.i("Message", responseData.message)
            Log.i("User Id", "${responseData.user_id}")
            Log.i("Name", responseData.name)
            Log.i("Email", responseData.email)
            Log.i("Mobile", "${responseData.mobile}")

            // Profile Details
            Log.i("Is Profile Completed", "${responseData.profile_details.is_profile_completed}")
            Log.i("Rating", "${responseData.profile_details.rating}")

            // Data List Details.
            Log.i("Data List Size", "${responseData.data_list.size}")

            for (item in responseData.data_list.indices) {
                Log.i("Value $item", "${responseData.data_list[item]}")

                Log.i("ID", "${responseData.data_list[item].id}")
                Log.i("Value", "${responseData.data_list[item].value}")
            }

            /**
             * Without GSON
             *Creates a new with name/value mappings from the JSON string.
             *//*
            val jsonObject = result?.let { JSONObject(it) }

            // Returns the value mapped by {name} if it exists.
            val message = jsonObject?.optString("message")
            if (message != null) {
                Log.i("Message", message)
            }
            val userId = jsonObject?.optInt("user_id")
            if (userId!= null) {
                Log.i("User_Id", "$userId")
            }

            val profileDetailsObject = jsonObject?.optJSONObject("profile_details")
            val isProfileCompleted = profileDetailsObject?.optBoolean("is_profile_completed")

            val dataListArray = jsonObject?.optJSONArray("data_list")
            for (item in 0 until dataListArray!!.length()) {
                val dataItem = dataListArray.optJSONObject(item)
                val dataItemId = dataItem.optInt("id")
                val dataItemValue = dataItem.optString("value")
                Log.i("Data_Item_Id", "$dataItemId")
                Log.i("Data_Item_Value", dataItemValue)
            }
            */
        }
            private fun showProgressDialog() {
                customProgressDialog = Dialog(this@MainActivity)
                customProgressDialog.setContentView(R.layout.dialog_custom_progress)
                customProgressDialog.show()
            }

            private fun cancelProgressDialog() {
                customProgressDialog.dismiss()
            }
    } */

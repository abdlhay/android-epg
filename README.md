## Project Background

This project is a fork of the [original repository](https://github.com/korre/android-tv-epg)

## Enhancements

In this fork, the following improvements have been made:

- Updated the Gradle version and dependencies to their latest stable versions.
- Converted the original Java codebase to Kotlin for better interoperability and modern language features.


# Electronic Program Guide for Android

[epg-recording.mp4](https://github.com/abdlhay/android-tv-epg/assets/44603158/91cd8484-6449-4784-9c63-01a792e52339)

This is a "classic" TV EPG which works on tablets and phones and allows you to scroll in all directions (horizontal, vertical and diagonal).
Example project is located in repo but in short you need to add the EPG to your xml or by code:

```xml
<com.abmo.tvepg.epg.EPG
        android:id="@+id/epg"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="@color/epg_background"/>
```

Then by code adding a click listener...

```kotlin
epg.setEPGClickListener(object : EPGClickListener {
            override fun onChannelClicked(
                channelPosition: Int, 
                epgChannel: EPGChannel?) {
                
            }
    
            override fun onEventClicked(
                channelPosition: Int,
                programPosition: Int,
                epgEvent: EPGEvent?) {
               
            }

            override fun onResetButtonClicked() {
               
            }
        })
```
... and data to be shown.

```kotlin
epg.setEPGData(EPGDataImpl(MockDataService.mockData))
```

That's basically it.
If you want to use it in your project you need resources from the example project as well as the epg package for it to work. If you have any questions or such don't hesitate to contact me.

Good luck!


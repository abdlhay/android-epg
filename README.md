# NOTE:

## Project Background

This project is a fork of the [original repository](https://github.com/korre/android-tv-epg)

## Enhancements

In this fork, the following improvements have been made:

- Updated the Gradle version and dependencies to their latest stable versions.
- Converted the original Java codebase to Kotlin for better interoperability and modern language features.


# Electronic Program Guide for Android

![Click for video](\TVepg\epg-recording.mp4)

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
            override fun onChannelClicked(channelPosition: Int, epgChannel: EPGChannel?) {
                Toast.makeText(
                    this@MainActivity,
                    epgChannel?.name + " clicked",
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun onEventClicked(
                channelPosition: Int,
                programPosition: Int,
                epgEvent: EPGEvent?
            ) {
                Toast.makeText(
                    this@MainActivity,
                    epgEvent?.title + " clicked",
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun onResetButtonClicked() {
                epg.recalculateAndRedraw(true)
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


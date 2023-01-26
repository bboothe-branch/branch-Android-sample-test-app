package com.example.branchandroidtestsampleapp

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import androidx.annotation.RequiresPermission
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.view.updateLayoutParams
import io.branch.referral.Branch
import io.branch.indexing.BranchUniversalObject
import io.branch.referral.BranchError
import io.branch.referral.QRCode.BranchQRCode
import io.branch.referral.SharingHelper
import io.branch.referral.util.*

import org.json.JSONObject
import org.w3c.dom.Text
import java.util.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onStart() {
        super.onStart()

        // ---------- Initialize Branch Session on App Open ----------
        Branch.sessionBuilder(this).withCallback { branchUniversalObject, linkProperties, error ->
            if (error != null) {
                Log.e("BranchSDK_Tester", "branch init failed. Caused by -" + error.message)
            } else {
                Log.e("BranchSDK_Tester", "branch init complete!")
                if (branchUniversalObject != null) {
                    Log.e("BranchSDK_Tester", "title " + branchUniversalObject.title)
                    Log.e("BranchSDK_Tester", "CanonicalIdentifier " + branchUniversalObject.canonicalIdentifier)
                    Log.e("BranchSDK_Tester", "metadata " + branchUniversalObject.contentMetadata.convertToJson())
                }
                if (linkProperties != null) {
                    Log.e("BranchSDK_Tester", "Channel " + linkProperties.channel)
                    Log.e("BranchSDK_Tester", "control params " + linkProperties.controlParams)
                }
            }
        }.withData(this.intent.data).init()

        // ---------- Create Branch Link ----------

        val buo = BranchUniversalObject()
            .setTitle("Title Link")
            .setContentDescription("Link created using the Branch SDK")

        val branchLinkTest = findViewById<TextView>(R.id.generatedLink)


        fun createBranchLink() {
            val lp = LinkProperties()
                .setCampaign("Sample Test App Campaign Example")
                .setChannel("Sample Test App Marketing")
                .setFeature("sharing")
                .addControlParameter("\$desktop_url", "http://example.com/home")
                .addControlParameter("\$deeplink_path", "deepLinkPath")

            buo.generateShortUrl(this, lp, Branch.BranchLinkCreateListener { url, error ->
                if (error == null) {
                    Log.i("BRANCH SDK", "Branch Link to share: " + url)
                    branchLinkTest.updateLayoutParams {
                        width = WRAP_CONTENT
                        height = WRAP_CONTENT
                    }
                    branchLinkTest.text = url.toString();
                }
            })

        }

        createBranchLink()

        // ---------- Share Branch Link ----------

        fun ShareBranchLink() {
            var lp = LinkProperties()
                .setChannel("facebook")
                .setFeature("sharing")
                .setCampaign("content 123 launch")
                .setStage("new user")
                .addControlParameter("\$desktop_url", "http://example.com/home")
                .addControlParameter("custom", "data")
                .addControlParameter("custom_random", Calendar.getInstance().getTimeInMillis().toString())

            val ss = ShareSheetStyle(this@MainActivity, "Check this out!", "This stuff is awesome: ")
                .setCopyUrlStyle(resources.getDrawable(androidx.appcompat.R.drawable.abc_ic_menu_copy_mtrl_am_alpha), "Copy", "Added to clipboard")
                .setMoreOptionStyle(resources.getDrawable(androidx.appcompat.R.drawable.abc_ic_search_api_material), "Show more")
                .addPreferredSharingOption(SharingHelper.SHARE_WITH.FACEBOOK)
                .addPreferredSharingOption(SharingHelper.SHARE_WITH.EMAIL)
                .addPreferredSharingOption(SharingHelper.SHARE_WITH.MESSAGE)
                .addPreferredSharingOption(SharingHelper.SHARE_WITH.HANGOUT)
                .setAsFullWidthStyle(true)
                .setSharingTitle("Share With")

            buo.showShareSheet(this, lp, ss, object : Branch.BranchLinkShareListener {
                override fun onShareLinkDialogLaunched() {}
                override fun onShareLinkDialogDismissed() {}
                override fun onLinkShareResponse(sharedLink: String?, sharedChannel: String?, error: BranchError?) {}
                override fun onChannelSelected(channelName: String) {}
            })
        }

        val shareButton = findViewById<ImageButton>(R.id.shareLinkButton)
        shareButton.setOnClickListener {
            ShareBranchLink()
        }


        // ---------- Read Deep Link Button ----------
        val readDeepLinkButton = findViewById<Button>(R.id.readDeepLinkButton)
        readDeepLinkButton.setOnClickListener {
            val readDeepLinkPageIntent = Intent(this, ReadDeepLinkActivity::class.java)
            startActivity(readDeepLinkPageIntent)
        }




        // ---------- Branch Event Buttons ----------

        val customEventButton = findViewById<Button>(R.id.purchaseStandardEventButton);
        customEventButton.setOnClickListener {
            BranchEvent(BRANCH_STANDARD_EVENT.INITIATE_PURCHASE).logEvent(this)
            Toast.makeText(this, "'INITIATE_PURCHASE' Commerce Event sent!", Toast.LENGTH_SHORT).show()
            println("'INITIATE_PURCHASE' Commerce Event sent!")
        }

        val commerceEventButton = findViewById<Button>(R.id.commerceEventButton)
        commerceEventButton.setOnClickListener {
            BranchEvent(BRANCH_STANDARD_EVENT.ADD_TO_CART).logEvent(this)
            Toast.makeText(this, "'ADD_TO_CARD' Commerce Event sent!", Toast.LENGTH_SHORT).show()
            println("'ADD_TO_CART' Commerce Event sent!")
        }

        val branchBadge = findViewById<ImageView>(R.id.branchBadgeDark)
        var iconMeter = 0
        val changeBannerEventButton = findViewById<Button>(R.id.changeBranchBadgeButton)
        changeBannerEventButton.setOnClickListener {
            if (iconMeter == 0) {
                branchBadge.setImageResource(R.drawable.branchbadgelightlarger)
                iconMeter++
            } else {
                branchBadge.setImageResource(R.drawable.branchcopybadgelarger)
                iconMeter--
            }
            BranchEvent("Change Badge").logEvent(this)
            Toast.makeText(this, "'Change Badge' Custom Event sent!", Toast.LENGTH_SHORT).show()
            println("'Change Badge' Custom Event sent!")
        }

        // ---------- Create QR Code ----------

        fun createQRCode() {
            val qrCode = BranchQRCode()
                .setCodeColor("#050E3C")
                .setBackgroundColor(Color.WHITE)
                .setMargin(1)
                .setWidth(512)
                .setImageFormat(BranchQRCode.BranchImageFormat.JPEG)
                .setCenterLogo("https://cdn.branch.io/branch-assets/1598575682753-og_image.png")

            val qrCodeLinkProperties = LinkProperties()
                .setChannel("QR Code Creator Channel")
                .setCampaign("QR Code Campaign")


            qrCode.getQRCodeAsImage(this@MainActivity, buo, qrCodeLinkProperties, object :
                BranchQRCode.BranchQRCodeImageHandler<Any?> {
                override fun onSuccess(qrCodeImage: Bitmap) {
                    //Do something with your QR code here.
                    val qrCodeImageLink = findViewById<ImageView>(R.id.qrCodeImageView)
                    qrCodeImageLink.updateLayoutParams {
                        width = WRAP_CONTENT
                        height = WRAP_CONTENT
                    }
                    qrCodeImageLink.setImageBitmap(qrCodeImage)

                }

                override fun onFailure(e: Exception) {
                    Log.d("Failed to get QR code", e.toString())
                }
            })
        }

        val createQRCodeButton = findViewById<ImageButton>(R.id.qrCodeIconImage)
        createQRCodeButton.setOnClickListener {
            createQRCode()
        }



       /* // Dynamic Shortcut

        val shortcut = ShortcutInfoCompat.Builder(this, "shortcut1")
            .setShortLabel("Testing")
            .setLongLabel("Testing for Help")
            .setIcon(IconCompat.createWithResource(this, R.drawable.branchbadgedark))
            .setIntent(Intent(this, ReadDeepLinkActivity::class.java))
            .build()

        ShortcutManagerCompat.pushDynamicShortcut(this, shortcut)*/
    }

    // ---------- Initialize New Branch Session ----------
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Branch.sessionBuilder(this).withCallback { referringParams, error ->
            if (error != null) {
                Log.e("BranchSDK_Tester", error.message)
            } else if (referringParams != null) {
                Log.e("BranchSDK_Tester", referringParams.toString())
                println("SDK LOGS ABOVE")
            }
        }.reInit()
    }
}
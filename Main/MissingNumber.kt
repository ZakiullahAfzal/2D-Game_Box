package com.example.a2dgamebox

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.media.Image
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.a2dgamebox.databinding.ActivityMissingNumberBinding

class MissingNumber : AppCompatActivity() {

    private lateinit var binding: ActivityMissingNumberBinding
    private lateinit var textView: Array<TextView?>
    private lateinit var imageViewLock: Array<ImageView?>
    private lateinit var linearLayout: Array<LinearLayout?>

    private lateinit var images: Array<Int>
    private lateinit var buttons: Array<Button>

    private var count: Int = 0
    private var level: Int = 0
    private var state: Int = 0

    private var getCoin: Boolean = true

    private var prevLevel : Int = 0


    override fun onCreate(savedInstanceState: Bundle?) {
         requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        super.onCreate(savedInstanceState)
        binding = ActivityMissingNumberBinding.inflate(layoutInflater)
        enableEdgeToEdge()

        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sharedPreferences = getSharedPreferences("gameBox2d", MODE_PRIVATE)
        sharedPreferencesMissingNumber = getSharedPreferences("missingNumber", MODE_PRIVATE)
        count = sharedPreferencesMissingNumber.getInt("count", 0)

        if (count < 50)
            level = count

        setAttribute()
        setActions()
    }

    private fun setAttribute() {
        linearLayout = arrayOf(
            binding.level1, binding.level2, binding.level3, binding.level4,
            binding.level5, binding.level6, binding.level7, binding.level8,
            binding.level9, binding.level10, binding.level11, binding.level12,
            binding.level13, binding.level14, binding.level15, binding.level16,
            binding.level17, binding.level18, binding.level19, binding.level20, binding.level21,
            binding.level22, binding.level23, binding.level24, binding.level25, binding.level26,
            binding.level27, binding.level28, binding.level29, binding.level30, binding.level31,
            binding.level32, binding.level33, binding.level34, binding.level35, binding.level36,
            binding.level37, binding.level38, binding.level39, binding.level40, binding.level41,
            binding.level42, binding.level43, binding.level44, binding.level45, binding.level46,
            binding.level47, binding.level48, binding.level49, binding.level50
        )

        textView = arrayOf(
            binding.text1,
            binding.text2,
            binding.text3,
            binding.text4,
            binding.text5,
            binding.text6,
            binding.text7,
            binding.text8,
            binding.text9,
            binding.text10,
            binding.text11,
            binding.text12,
            binding.text13,
            binding.text14,
            binding.text15,
            binding.text16,
            binding.text17,
            binding.text18,
            binding.text19,
            binding.text20,
            binding.text21,
            binding.text22,
            binding.text23,
            binding.text24,
            binding.text25,
            binding.text26,
            binding.text27,
            binding.text28,
            binding.text29,
            binding.text30,
            binding.text31,
            binding.text32,
            binding.text33,
            binding.text34,
            binding.text35,
            binding.text36,
            binding.text37,
            binding.text38,
            binding.text39,
            binding.text40,
            binding.text41,
            binding.text42,
            binding.text43,
            binding.text44,
            binding.text45,
            binding.text46,
            binding.text47,
            binding.text48,
            binding.text49,
            binding.text50
        )

        imageViewLock = arrayOf(
            binding.mnLock1,
            binding.mnLock2,
            binding.mnLock3,
            binding.mnLock4,
            binding.mnLock5,
            binding.mnLock6,
            binding.mnLock7,
            binding.mnLock8,
            binding.mnLock9,
            binding.mnLock10,
            binding.mnLock11,
            binding.mnLock12,
            binding.mnLock13,
            binding.mnLock14,
            binding.mnLock15,
            binding.mnLock16,
            binding.mnLock17,
            binding.mnLock18,
            binding.mnLock19,
            binding.mnLock20,
            binding.mnLock21,
            binding.mnLock22,
            binding.mnLock23,
            binding.mnLock24,
            binding.mnLock25,
            binding.mnLock26,
            binding.mnLock27,
            binding.mnLock28,
            binding.mnLock29,
            binding.mnLock30,
            binding.mnLock31,
            binding.mnLock32,
            binding.mnLock33,
            binding.mnLock34,
            binding.mnLock35,
            binding.mnLock36,
            binding.mnLock37,
            binding.mnLock38,
            binding.mnLock39,
            binding.mnLock40,
            binding.mnLock41,
            binding.mnLock42,
            binding.mnLock43,
            binding.mnLock44,
            binding.mnLock45,
            binding.mnLock46,
            binding.mnLock47,
            binding.mnLock48,
            binding.mnLock49,
            binding.mnLock50
        )

        images = arrayOf(
            R.drawable.missingnumber1,
            R.drawable.missingnumber2,
            R.drawable.missingnumber3,
            R.drawable.missingnumber4,
            R.drawable.missingnumber5,
            R.drawable.missingnumber6,
            R.drawable.missingnumber7,
            R.drawable.missingnumber8,
            R.drawable.missingnumber9,
            R.drawable.missingnumber10,
            R.drawable.missingnumber11,
            R.drawable.missingnumber12,
            R.drawable.missingnumber13,
            R.drawable.missingnumber14,
            R.drawable.missingnumber15,
            R.drawable.missingnumber16,
            R.drawable.missingnumber17,
            R.drawable.missingnumber18,
            R.drawable.missingnumber19,
            R.drawable.missingnumber20,
            R.drawable.missingnumber21,
            R.drawable.missingnumber22,
            R.drawable.missingnumber23,
            R.drawable.missingnumber24,
            R.drawable.missingnumber25,
            R.drawable.missingnumber26,
            R.drawable.missingnumber27,
            R.drawable.missingnumber28,
            R.drawable.missingnumber29,
            R.drawable.missingnumber30,
            R.drawable.missingnumber31,
            R.drawable.missingnumber32,
            R.drawable.missingnumber33,
            R.drawable.missingnumber34,
            R.drawable.missingnumber35,
            R.drawable.missingnumber36,
            R.drawable.missingnumber37,
            R.drawable.missingnumber38,
            R.drawable.missingnumber39,
            R.drawable.missingnumber40,
            R.drawable.missingnumber41,
            R.drawable.missingnumber42,
            R.drawable.missingnumber43,
            R.drawable.missingnumber44,
            R.drawable.missingnumber45,
            R.drawable.missingnumber46,
            R.drawable.missingnumber47,
            R.drawable.missingnumber48,
            R.drawable.missingnumber49,
            R.drawable.missingnumber50
        )

        buttons = arrayOf(
            binding.mpDigit1,
            binding.mpDigit2,
            binding.mpDigit3,
            binding.mpDigit4,
            binding.mpDigit5,
            binding.mpDigit6,
            binding.mpDigit7,
            binding.mpDigit8,
            binding.mpDigit9,
            binding.mpDigit0
        )


    }

    @SuppressLint("SetTextI18n")
    private fun setActions() {

        binding.backButton?.setOnClickListener {
            finish()
        }

        binding.mpBack.setOnClickListener {
            setBack()
        }

        binding.mpHint.setOnClickListener {

        }

        showLevels()

        for(i in buttons.indices){
            buttons[i].setOnClickListener {
                if (binding.mpAnswer.text.toString().length < 8) {
                    val state = binding.mpAnswer.text.toString()
                    binding.mpAnswer.text =  if(i == 9) state + "0" else state + (i + 1)
                }else Toast.makeText(this, "Out of space", Toast.LENGTH_SHORT).show()
            }
        }

        binding.mpEnter.setOnClickListener {
            if (binding.mpAnswer.text.toString() == ans[level]) {
                if (level == count && count < 50) {
                    prevLevel = count
                    setPrevious()
                    count++
                    level = count
                    edit()
                } else if(level < 50)
                    level++
                binding.mpLevel.text = "${level + 1}/50"
                nextLevel()
            } else {
                Toast.makeText(this, "Wrong Answer!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.textViewNextLevel.setOnClickListener {
            if (level < images.size) {
                reMain()
                setImage(images[level])
            } else {
                binding.textViewNextLevel.text = "Level Complete"
            }
        }

        binding.mpClear.setOnClickListener {
            binding.mpAnswer.text = ""
        }

        binding.mpDelete.setOnClickListener {
           val state = binding.mpAnswer.text
           if (state.isNotEmpty()) binding.mpAnswer.text = state.substring(0, state.length-1)
        }


        binding.mpHint.setOnClickListener {
            showHelpView(true)
        }
        binding.mpBackHelp.setOnClickListener {
             showHelpView(false)
        }
        binding.textViewPruches.setOnClickListener {
            val coin = sharedPreferences.getInt("coin", 0)
            if (coin < 50) Toast.makeText(this, "No Coin left", Toast.LENGTH_SHORT).show()
            else {
                if(getCoin) {
                    getCoin = false
                    val state = coin - 50
                    binding.textViewAnswer.text = solution[level]
                    binding.textViewAnswer.visibility = View.VISIBLE
                    sharedPreferences.edit().putInt("coin", state).apply()
                }else Toast.makeText(this, "One Time Pouches", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun showHelpView(state: Boolean) {
        if (state) {
            binding.linearLayoutPlayGround.visibility = View.INVISIBLE
            binding.textViewPruches.visibility = View.VISIBLE
            binding.imageViewPruchesCoin.visibility = View.VISIBLE
            binding.textViewMessage.visibility = View.VISIBLE
            binding.textViewAnswer.visibility = View.VISIBLE
            binding.mpBackHelp.visibility = View.VISIBLE
        } else {
            binding.linearLayoutPlayGround.visibility = View.VISIBLE
            binding.textViewPruches.visibility = View.INVISIBLE
            binding.imageViewPruchesCoin.visibility = View.INVISIBLE
            binding.textViewMessage.visibility = View.INVISIBLE
            binding.textViewAnswer.visibility = View.INVISIBLE
            binding.mpBackHelp.visibility = View.INVISIBLE


        }
    }
    @SuppressLint("SetTextI18n")
    private fun showLevels(){
        for (i in 0..count) {
            textView[i]?.setTextColor(Color.WHITE)
            imageViewLock[i]?.setImageResource(R.drawable.mn_play)
            linearLayout[i]?.setOnClickListener {
                setResume()
                level = i
                state = 1
                if (level < images.size) {
                    binding.mpLevel.text = "${level + 1}/50"
                    setImage(images[level])
                }
            }
        }

        setPrevious()

    }

    private fun setPrevious(){
        if (prevLevel <= count){
            linearLayout[count]?.setBackgroundResource(R.drawable.mn_open)
            linearLayout[prevLevel]?.setBackgroundResource(R.drawable.mn_locked)
        }
    }

    private fun reMain(){
        binding.imageViewNextLevel.visibility = View.INVISIBLE
        binding.textViewNextLevel.visibility = View.INVISIBLE
        binding.linearLayoutPlayGround.visibility = View.VISIBLE


    }
    private fun nextLevel(){
        binding.linearLayoutPlayGround.visibility = View.INVISIBLE
        binding.imageViewNextLevel.visibility = View.VISIBLE
        binding.textViewNextLevel.visibility = View.VISIBLE
        binding.mpAnswer.text = ""
    }

    private fun edit() {
       sharedPreferencesMissingNumber.edit().putInt("count", count).apply()
    }

    private fun setImage(resourceId: Int) {
        binding.qustion.setImageResource(resourceId)
    }

    private fun setResume() {
        binding.linearLayoutTitle?.visibility = View.INVISIBLE
        binding.scrollViewLevels?.visibility = View.INVISIBLE

        binding.linearLayoutPlayGround.visibility = View.VISIBLE
    }

    private fun setBack(){
        showLevels()
        binding.linearLayoutTitle?.visibility = View.VISIBLE
        binding.scrollViewLevels?.visibility = View.VISIBLE

        binding.linearLayoutPlayGround.visibility = View.INVISIBLE
    }

    companion object {
        private lateinit var sharedPreferencesMissingNumber: SharedPreferences
        private lateinit var sharedPreferences: SharedPreferences

        private var solution = arrayOf(
            "(1*2) (2*2) (4*2).. = 16",//1
            "3",//2
            "3",//3
            "n * 5 = 10",//4
            "n + 8 = 31",//5
            "14",//6
            "[] = 10",//7
            "A : 60 B : 20 = 3",//8
            "n + m = 78",//9
            "2",//10
            "96004",//11
            "n+5+6+7 = 31",//12
            "n*m = 56",//13
            "10",//14
            "529",//15
            "n * (n+1) = 110",//16
            "n*m (m = 7)",//17
            "n+10 + 20 + 30 = 69",//18
            "1(power) 2(/) 3(+) 4(-) = 1",//19
            "n+10 - 3 = 17",//20
            "3186",//21
            "n*2 +1 = 61",//22
            "8 * 8 = 64",//23
            "n*m+3 = 84",//24
            "(n-2) / 2 = 5",//25
            "13 + 5 = 18",//26
            "6+3 = 9",//27
            "(n+m)^2 = 64",//28
            "7 * 8 = 56",//29
            "n - 60 = 203",//30
            "(n^2)+m = 17",//31
            "1+2 = 3",//32
            "(n*n) + (m*m) = 26",//33
            "n*3 : 3",//34
            "(n+m)/2 = 4",//35
            "1+2 = 3",//36
            "n*m + (n+m) = 23",//37
            "47 <-> 74 = 81",//38
            "n+10 - 5 = 16",//39
            "n+5 = 2",//40
            "5^5 7^7 : 7+5 = 12",//41
            "(20 + 7) / 3 = 9",//42
            "(n+1) * (m +1) = 48",//43
            "(2-1) (7-1) (1-1) = 160",//44
            "11 * 5 = 55",//45
            "(4*4) - (3*3) = 7",//46
            "7-2 5-2 3-2 = 1",//47
            "4+1 4-1 = 53",//48
            "2*7 = 14",//49
            "2*(2-1) 3*(3-1)..6*(6-1)=30"//50

        )

        private var ans = arrayOf(
            "16", //1
            "3", //2
            "3", //3
            "10",//4
            "31",//5
            "14",//6
            "10",//7
            "3",//8
            "78",//9
            "2",//10
            "96004",//11
            "31",//12
            "56",//13
            "10",//14
            "529",//15
            "110",//16
            "7",//17
            "69",//18
            "1",//19
            "17",//20
            "3186", //21
            "61", //22
            "64",//23
            "84",//24
            "5",//25
            "18",//26
            "9",//27
            "64",//28
            "56",//29
            "203",//30
            "17",//31
            "3",//32
            "26",//33
            "3",//34
            "4",//35
            "1",//36
            "23",//37
            "81",//38
            "16",//39
            "2",//40
            "12",//41
            "9",//42
            "48",//43
            "160",//44
            "55",//45
            "7",//46
            "1",//47
            "53",//48
            "2",//49
            "30"//50
        )
    }
}
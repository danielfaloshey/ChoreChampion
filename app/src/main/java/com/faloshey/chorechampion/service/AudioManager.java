package com.faloshey.chorechampion.service;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import com.faloshey.chorechampion.R;
import java.util.HashMap;

public class AudioManager {

    private static AudioManager instance;

    private MediaPlayer themePlayer;
    private SoundPool soundPool;
    private final HashMap<String, Integer> soundMap;
    private boolean isInitialized = false;

    private AudioManager() {
        soundMap = new HashMap<>();
    }

    public static synchronized AudioManager getInstance() {
        if (instance == null) {
            instance = new AudioManager();
        }
        return instance;
    }

    public void init(Context context) {
        if (isInitialized) return;

        Context appContext = context.getApplicationContext();

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(audioAttributes)
                .build();

        soundMap.put("cork_pop", soundPool.load(appContext, R.raw.cork_pop, 1));
        soundMap.put("quest_complete", soundPool.load(appContext, R.raw.quest_complete, 1));
        soundMap.put("purchase_complete", soundPool.load(appContext, R.raw.purchase_complete, 1));

        themePlayer = MediaPlayer.create(appContext, R.raw.medieval_theme);
        if (themePlayer != null) {
            themePlayer.setLooping(true);
            themePlayer.setVolume(0.4f, 0.4f);
        }

        isInitialized = true;
    }


    /* Sound FX Actions */
    public void playSound(String soundKey) {
        if (!isInitialized || soundPool == null) return;

        Integer soundId = soundMap.get(soundKey);
        if (soundId != null) {
            soundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f);
        }
    }

    /* Theme Music Actions */
    public void startTheme() {
        if (themePlayer != null && !themePlayer.isPlaying()) {
            themePlayer.start();
        }
    }

    public void pauseTheme() {
        if (themePlayer != null && themePlayer.isPlaying()) {
            themePlayer.pause();
        }
    }

    public void setMusicVolume(int volumeProgress) {
        if (themePlayer != null) {
            float volumeValue = volumeProgress / 100f;
            themePlayer.setVolume(volumeValue, volumeValue);
        }
    }
}

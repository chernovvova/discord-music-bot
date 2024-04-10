package com.musicbot;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

public final class TrackScheduler extends AudioEventAdapter {

    private final AudioPlayer player;
    private final BlockingQueue<AudioTrack> queue;

    public TrackScheduler(final AudioPlayer player) {
        this.player = player;
        this.queue = new LinkedBlockingQueue<>();
    }
    
    public String queue(AudioTrack track) {
        if(!player.startTrack(track, true)) {
            queue.offer(track);
            return "Adding to queue: " + track.getInfo().title + "\n" + track.getInfo().uri;
        }
        return "Playing: " + track.getInfo().title + "\n" + track.getInfo().uri;
    }

    public String nextTrack() {
        AudioTrack track = queue.poll();
        player.startTrack(track, false);
        if(track != null){
            return "Playing: " + track.getInfo().title;
        }
        return null;
    }

    public String queueToString() {
        String queueTracks = "";
        int position = 1;
        for(AudioTrack track : queue) {
            queueTracks += position + " " + track.getInfo().title + "\n";
            position++;
        }
        return queueTracks;
    }

    @Override
    public void onPlayerPause(AudioPlayer player) {
        
    }
 
    @Override
    public void onPlayerResume(AudioPlayer player) {

    }
    
    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
    }
    
    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if(endReason.mayStartNext) {
            nextTrack();
        }
    }
 
    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
    }
 
    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
    }
 
    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs, StackTraceElement[] stackTrace) {
       this.onTrackStuck(player, track, thresholdMs);
    }
}
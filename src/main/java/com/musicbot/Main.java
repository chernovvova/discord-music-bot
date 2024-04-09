package com.musicbot;

import java.util.HashMap;
import java.util.Map;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class Main extends ListenerAdapter{
    private final AudioPlayerManager playerManager;
    private final Map<Long, GuildMusicManager> musicManagers;
    public static void main(String[] args) {
        JDA api = JDABuilder.createDefault(Config.TOKEN)
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                    .addEventListeners(new Main())
                    .build();
    }

    private Main() {
        this.playerManager = new DefaultAudioPlayerManager();
        this.musicManagers = new HashMap<>();

        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);
    }

    private synchronized GuildMusicManager getGuildAudioPlayer(Guild guild) {
        long guildId = Long.parseLong(guild.getId());
        GuildMusicManager musicManager = musicManagers.get(guildId);

        if (musicManager == null) {
            musicManager = new GuildMusicManager(playerManager);
            musicManagers.put(guildId, musicManager);
        }

        guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());

        return musicManager;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        String[] command = event.getMessage().getContentRaw().split(" ", 2);

        if(command[0].equals("!play")) {
            VoiceChannel voiceChannel = event.getMember().getVoiceState().getChannel().asVoiceChannel();
            loadAndPlay(event.getChannel().asTextChannel(), command[1], voiceChannel);
        }
        else if(command[0].equals("!skip")) {
            skip(event.getChannel().asTextChannel());
        }
        else if(command[0].equals("!queue")) {
            String trackQueue = getQueue(event.getChannel().asTextChannel());
            event.getChannel().asTextChannel().sendMessage(trackQueue).queue();
        }
    }

    private void loadAndPlay(TextChannel textChannel, String trackUrl, VoiceChannel voiceChannel) {
        GuildMusicManager guildMusicManager = getGuildAudioPlayer(textChannel.getGuild());

        playerManager.loadItemOrdered(guildMusicManager, trackUrl, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                play(textChannel, guildMusicManager, track, voiceChannel);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                for(AudioTrack track : playlist.getTracks()) {
                    play(textChannel, guildMusicManager, track, voiceChannel);
                }
            }

            @Override
            public void noMatches() {
                
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                
            }
        });
    }

    private synchronized void play(TextChannel textChannel, GuildMusicManager guildMusicManager, AudioTrack track, VoiceChannel voiceChannel) {
        Guild guild = textChannel.getGuild();
        connectToVoiceChannel(guild.getAudioManager(), voiceChannel);
        String text = guildMusicManager.scheduler.queue(track);
        System.out.println(text);
        textChannel.sendMessage(text).queue();
    }

    private void skip(TextChannel textChannel) {
        GuildMusicManager musicManager = getGuildAudioPlayer(textChannel.getGuild());

        String skipText = musicManager.scheduler.nextTrack();
        if(skipText == null) {
            textChannel.sendMessage("Nothing is playing").queue();
        }
        else {
            textChannel.sendMessage("Skipped to next track").queue();
        }
    }
    
    private String getQueue(TextChannel textChannel) {
        GuildMusicManager musicManager = getGuildAudioPlayer(textChannel.getGuild());
        String trackList = musicManager.scheduler.queueToString();
        return trackList;
    }

    private static void connectToVoiceChannel(AudioManager audioManager, VoiceChannel voiceChannel) {
        if(!audioManager.isConnected()) {
            audioManager.openAudioConnection(voiceChannel);
        }
    }
}
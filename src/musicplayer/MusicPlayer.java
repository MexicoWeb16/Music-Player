/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package musicplayer;

import java.awt.Dimension;
import java.awt.Toolkit;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.media.*;
import javafx.stage.Stage;

import java.io.*;
import java.net.URL;
import java.util.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import javafx.util.StringConverter;
/**
 *
 * @author Agustino Flores
 * 
 * First version of the LMTC Music Player. 
 * I received help from 
 * https://blog.idrsolutions.com/2015/04/javafx-mp3-music-player-embedding-sound-in-your-application/
 * with this project.
 * If you are using this code, please give credits to myself and the other contributers for this project.
 */
public class MusicPlayer extends Application 
{
    //Variables used to find the location of the songs. You can change the directory.
    private URL url = getClass().getProtectionDomain().getCodeSource().getLocation();
    private File location = new File(url.getPath());
    private String fileURL = url.getPath().substring(0, url.getPath().indexOf(location.getParentFile().getParentFile().getName()));
    private final File dir = new File(fileURL + "/Songs");
    private List<File> files = getFiles(dir.listFiles());
    //end of those variables
    //stores the media players that hold all the songs found in the directory
    private List<MediaPlayer> players = createMediaPlayers();
    //controls the amount of songs played
    private int amountPlayed = 1;
    //variables used to keep track of the songs that just played or is currently playing
    private int currentSong = (int)(Math.random() * (files.size()));
    private int previousSong = currentSong;
    //end of those variables
    //used to see if the user wants the songs shuffled or not
    private boolean shuffle = false;
    //holds used to hold the current song playing
    private MediaPlayer curPlayer = players.get(currentSong);
    private MediaView view = new MediaView(curPlayer);
    
    //variables used to display the title of the song, progress of the song
    private final Label currentlyPlaying = new Label();
    private final ProgressBar progress = new ProgressBar();
    private final TableView<Map> metadataTable = new TableView<>();
    private ChangeListener<Duration> progressChangeListener = (ObservableValue<? extends Duration> observableValue, Duration oldValue, Duration newValue) -> {
            progress.setProgress(1.0 * curPlayer.getCurrentTime().toMillis() / curPlayer.getTotalDuration().toMillis());
        };
    private MapChangeListener<String, Object> metadataChangeListener = new MapChangeListener<String, Object>() {
            public void onChanged(Change<? extends String, ?> change) {
                metadataTable.getItems().setAll(convertMetadataToTableData(curPlayer.getMedia().getMetadata()));
        }
        };
    //used to control the volume
    private Slider volumeSlider = new Slider(0, 1, 0);
    
    @Override
    public void start(Stage primaryStage) 
    {
        //gets the songs from the directory
        createMediaPlayers();
        
        //creates the stage. Used BorderPane for easier design. 
        BorderPane root = new BorderPane();
        root.setBottom(addToolBar());
        VBox playlist = addPlaylist();
        root.setRight(playlist);
        root.setCenter(centerView());
        root.setLeft(addFilesMenu(root));
        
        //sets the screensize to be compatible with every screen
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        double width = screenSize.getWidth();
        double height = screenSize.getHeight();
        Scene scene = new Scene(root, width - 25, height - 70);
        
        primaryStage.setTitle("LMTC Music Player");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) 
    {
        launch(args);
    }
    
    //used to create the various media players by obtaining the song files from the directory
    private List<MediaPlayer> createMediaPlayers()
    {
      List<MediaPlayer> players = new ArrayList<MediaPlayer>();  
      for(int i = 0; i < files.size(); i++)  
      {    
        Media media = new Media(files.get(i).toURI().toString());
        MediaPlayer player = new MediaPlayer(media);
        players.add(player);
      }
      
      return players;
    }
    
    //creates the centerview of the player, consists of a gif, name of the song currently playing and controls the MediaView to play all the songs
    private VBox centerView()
    {
        VBox box = new VBox();
        box.setPadding(new Insets(60));
        box.setAlignment(Pos.TOP_RIGHT);
        box.alignmentProperty().isBound();
        box.setSpacing(5);
        box.setStyle("-fx-background-image: url('file:" + fileURL + "/images/background.jpg')");
        
        Image nowPlaying = new Image("file:" + fileURL + "/images/hexwave.gif");
        ImageView image = new ImageView(nowPlaying);
        image.setFitHeight(nowPlaying.getHeight() / 2);
        image.setFitWidth(nowPlaying.getWidth() / 2);
        box.getChildren().add(image);
        
        
        
        curPlayer = players.get(currentSong);
        view = new MediaView(curPlayer);
        
        while(amountPlayed < players.size())
        {
            curPlayer.setOnEndOfMedia(() -> {
                changeCurrentSong();
                songChange();
            });
            amountPlayed++;
            
        }
        view.mediaPlayerProperty().addListener((ObservableValue<? extends MediaPlayer> observableValue, MediaPlayer oldPlayer, MediaPlayer newPlayer) -> {
            setCurrentlyPlaying(newPlayer);
            
        });
        
        view.setMediaPlayer(curPlayer);
        view.getMediaPlayer().play();
        curPlayer.volumeProperty().bindBidirectional(volumeSlider.valueProperty());
        setCurrentlyPlaying(view.getMediaPlayer());
        
        box.getChildren().addAll(view, currentlyPlaying);
        return box;
    }
    
    //used to create the play, pause, shuffle, rewind, and forward buttons. uses images that I created myself. sets the actions for the buttons
    private HBox addToolBar()
    {
        HBox toolBar = new HBox();
        toolBar.setPadding(new Insets(20));
        toolBar.setAlignment(Pos.CENTER);
        toolBar.alignmentProperty().isBound();
        toolBar.setSpacing(5);
        toolBar.setStyle("-fx-background-image: url('file:" + fileURL + "/images/background.jpg')");
        
        Button playButton = addButton(new Image("file:" + fileURL + "/images/play.png"));
        playButton.setStyle("-fx-background-color: Gold");
        Button pauseButton = addButton(new Image("file:" + fileURL + "/images/pause.png"));
        Button forwardButton = addButton(new Image("file:" + fileURL + "/images/fastforward.png"));
        Button rewindButton = addButton(new Image("file:" + fileURL + "/images/rewind.png"));
        Button shuffleButton = addButton(new Image("file:" + fileURL + "/images/shuffle.png"));
        
        playButton.setOnAction((ActionEvent e) -> {
            view.getMediaPlayer().play();
            playButton.setStyle("-fx-background-color: Gold");
            pauseButton.setStyle("-fx-background-color: Black");
        });
        
        pauseButton.setOnAction((ActionEvent e) -> {
            view.getMediaPlayer().pause();
            pauseButton.setStyle("-fx-background-color: Gold");
            playButton.setStyle("-fx-background-color: Black");
        });
        
        shuffleButton.setOnAction((ActionEvent event) -> {
            shuffle = !shuffle;
            if(shuffle)
                shuffleButton.setStyle("-fx-background-color: Gold");
            else
                shuffleButton.setStyle("-fx-background-color: Black");
            
        });
        forwardButton.setOnAction((ActionEvent e) -> {
            changeCurrentSong();
            songChange();
        });
        rewindButton.setOnAction((ActionEvent e) -> {
           currentSong = previousSong;
           songChange();
        });
        toolBar.getChildren().addAll(rewindButton, playButton, pauseButton, forwardButton, shuffleButton, progress);
        
        return toolBar;
    }
    
    //creates the playlist that holds all the song names. Click on a song name and the song will play
    private VBox addPlaylist()
    {
        VBox playBar = new VBox();
        playBar.setPadding(new Insets(60));
        playBar.setAlignment(Pos.TOP_RIGHT);
        playBar.alignmentProperty().isBound();
        playBar.setSpacing(5);
        playBar.setStyle("-fx-background-image: url('file:" + fileURL + "/images/background.jpg')");
        Text text;
        if(files == null || files.size() == 0)
        {
            text = new Text("No files in directory. Add songs!");
            text.setFont(Font.font("Times New Roman", 16));
            text.setFill(Color.GOLD);
            playBar.getChildren().add(text);
        }    
        else
        {
            Text title = new Text("Your Songs Here!");
            title.setStyle("-fx-font-size: 21");
            title.setUnderline(true);
            title.setFill(Color.GOLD);
            title.setTextAlignment(TextAlignment.LEFT);
            playBar.getChildren().add(title);
            for(int i = 0; i < files.size(); i++)
            {
                String name = files.get(i).getName();
                text = new Text(10, 10, name);
                text.setOnMouseClicked((MouseEvent e) -> {
                    currentSong = search(name);
                    
                    songChange();
                });
                text.setFill(Color.GOLD);
                text.setTextAlignment(TextAlignment.LEFT);
                playBar.getChildren().add(text);
            }
        }
        return playBar;
    }
    
    
    private VBox addFilesMenu(BorderPane pane)
    {
        FileChooser.ExtensionFilter ext = new FileChooser.ExtensionFilter("Song files", "*.mp3", "*.wav", "*.m4a");
        FileChooser openFile = new FileChooser();
        openFile.getExtensionFilters().add(ext);
        
        VBox menu = new VBox();
        menu.setPadding(new Insets(30));
        menu.setAlignment(Pos.TOP_LEFT);
        menu.alignmentProperty().isBound();
        menu.setSpacing(5);
        menu.setStyle("-fx-background-image: url('file:" + fileURL + "/images/background.jpg')");
        
        Text text = new Text("Add a new song here!");
        text.setFont(Font.font("Times New Roman", 16));
        text.setFill(Color.GOLD);
        Text newSong = new Text();
        newSong.setFill(Color.GOLD);
        Button addFile = new Button("Add New File");
        addFile.setStyle("-fx-background-color: Gold;");
        addFile.setTextFill(Color.WHITE);
        addFile.setOnAction((ActionEvent e) -> {
            File newFile = openFile.showOpenDialog(null);
            try
            {    
                newFile.renameTo(new File(dir.toString() + "/" + newFile.getName()));
            }
            catch (Exception ex)
            { 
               ex.printStackTrace();
            }
            files.add(new File(dir.toString() + "/" + newFile.getName()));
            players = createMediaPlayers();
            pane.setRight(addPlaylist());
            //newSong.setText(newFile.getName());
        });
        
        Text volumeHeading = new Text("Volume");
        volumeHeading.setFont(Font.font("Times New Roman", 16));
        volumeHeading.setFill(Color.GOLD);
        volumeHeading.setUnderline(true);
        volumeSlider.setOrientation(Orientation.VERTICAL);
        volumeSlider.setValue(.5);
        volumeSlider.setStyle("-fx-color: Gold");
        Text volumePercent = new Text(volumeSlider.valueProperty().doubleValue() * 100 + "%");
        volumePercent.setFont(Font.font("Times New Roman", 16));
        volumePercent.setFill(Color.GOLD);

        
        volumePercent.textProperty().bindBidirectional(volumeSlider.valueProperty(), new StringConverter<Number>()
        {

        @Override
        public String toString(Number t)
        {
            double value = t.doubleValue() * 100;
            int cutValue = (int) value;
            return cutValue + "%";
        }

        @Override
        public Number fromString(String string)
        {
            return Double.parseDouble(string);
        }

        });

        menu.getChildren().addAll(text, addFile,volumeHeading, volumeSlider, volumePercent);
        
        return menu;
    }
    
    private Button addButton(Image img)
    {
        Button button = new Button();
        button.setGraphic(new ImageView(img));
        button.setStyle("-fx-background-color: Black");
        
        return button;
    }
    
    private void setCurrentlyPlaying(MediaPlayer newPlayer) 
    {
        newPlayer.seek(Duration.ZERO);

        progress.setProgress(0);
        progressChangeListener = (ObservableValue<? extends Duration> observableValue, Duration oldValue, Duration newValue) -> {
            progress.setProgress(1.0 * newPlayer.getCurrentTime().toMillis() / newPlayer.getTotalDuration().toMillis());
        };
        newPlayer.currentTimeProperty().addListener(progressChangeListener);

        String source = newPlayer.getMedia().getSource();
        source = source.substring(0, source.length() - 3);
        source = source.substring(source.lastIndexOf("/") + 1).replaceAll("%20", " ");
        currentlyPlaying.setText("Now Playing: " + source);
        currentlyPlaying.setTextFill(Color.web("gold"));
        
        setMetaDataDisplay(newPlayer.getMedia().getMetadata());
    }
    
    private void setMetaDataDisplay(ObservableMap<String, Object> metadata) 
    {
        metadataTable.getItems().setAll(convertMetadataToTableData(metadata));
        metadataChangeListener = new MapChangeListener<String, Object>() {
            public void onChanged(Change<? extends String, ?> change) {
                metadataTable.getItems().setAll(convertMetadataToTableData(metadata));
        }
        };
        metadata.addListener(metadataChangeListener);
   }
   private ObservableList<Map> convertMetadataToTableData(ObservableMap<String, Object> metadata) 
   {
    ObservableList<Map> allData = FXCollections.observableArrayList();

    for (String key: metadata.keySet()) {
      Map<String, Object> dataRow = new HashMap<>();

      dataRow.put("Tag",   key);
      dataRow.put("Value", metadata.get(key));

      allData.add(dataRow);
    }

    return allData;
  } 
    private void changeCurrentSong()
    {
      previousSong = currentSong;
      if(shuffle)
      {    
        currentSong = (int)(Math.random() * (files.size()));
        while(currentSong == previousSong)
            currentSong = (int)(Math.random() * (files.size()));
      }
      else
      {
        if(currentSong == players.size() - 1)
           currentSong = 0;
        else
            currentSong += 1;
      }  
    }
    
    private void songChange()
    {
        curPlayer.currentTimeProperty().removeListener(progressChangeListener);
        curPlayer.getMedia().getMetadata().removeListener(metadataChangeListener);
        curPlayer.stop();
                
        curPlayer = players.get(currentSong);
        view = new MediaView(curPlayer);
        curPlayer.play();
        
        curPlayer.volumeProperty().bindBidirectional(volumeSlider.valueProperty());
        setCurrentlyPlaying(curPlayer);
    }
    
    private int search(String song)
    {
        for(int i = 0; i < players.size(); i++)
            if(song.equals(files.get(i).getName()))
                return i;
        return -1;
    }
    
    private List<File> getFiles(File[] files)
    {
        if(!dir.exists())
            dir.mkdir();
        List<File> songs = new ArrayList<File>();
        if(files == null || files.length == 0)
            return songs;
        
        for(File file : files)
            songs.add(file);

        return songs;
    }

}

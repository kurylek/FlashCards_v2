//TODO: Count as ArrayList size
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


public class Main extends Application{
    public static void main(String[] args){
        launch(args);
    }

    private Stage stWindow;
    private Scene scMenu, scPreview, scGame;
    private TableView<FlashCardsSet> tvFlashCardsSets;
    private TableView<FlashCard> tvFlashCards;
    private boolean unsavedChanges = false;
    private Button bSave;

    @Override
    public void start(Stage stage) throws Exception{
        stWindow = stage;
        stWindow.setTitle("FlashCards");

        GridPane gpMenu = new GridPane();

        TableColumn<FlashCardsSet, String> tcName = new TableColumn<>("Name");
        tcName.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<FlashCardsSet, String> tcDescription = new TableColumn<>("Description");
        tcDescription.setCellValueFactory(new PropertyValueFactory<>("description"));

        TableColumn<FlashCardsSet, Integer> tcCount = new TableColumn<>("Count");
        tcCount.setCellValueFactory(new PropertyValueFactory<>("count"));

        tvFlashCardsSets = new TableView();
        loadSets();
        tvFlashCardsSets.getColumns().addAll(tcName, tcDescription, tcCount);

        gpMenu.add(tvFlashCardsSets, 0 ,0, 5, 1);


        Button bCreate = new Button("Create");
        bCreate.setOnAction(e -> addNewSet());
        gpMenu.add(bCreate, 0, 1);

        Button bDelete = new Button("Delete");
        bDelete.setOnAction(e -> deleteSet(tvFlashCardsSets.getSelectionModel().getSelectedItem()));
        bDelete.disableProperty().bind(Bindings.isEmpty(tvFlashCardsSets.getSelectionModel().getSelectedItems()));
        gpMenu.add(bDelete, 1, 1);

        Button bEdit = new Button("Edit");
        bEdit.setOnAction(e -> editSet(tvFlashCardsSets.getSelectionModel().getSelectedItem()));
        bEdit.disableProperty().bind(Bindings.isEmpty(tvFlashCardsSets.getSelectionModel().getSelectedItems()));
        gpMenu.add(bEdit, 2, 1);

        bSave = new Button("Save");
        bSave.setOnAction(e -> saveSets());
        gpMenu.add(bSave, 3, 1);
        bSave.setDisable(true);

        Button bPreview = new Button("Preview");
        bPreview.setOnAction(e -> previewSet(tvFlashCardsSets.getSelectionModel().getSelectedItem()));
        bPreview.disableProperty().bind(Bindings.isEmpty(tvFlashCardsSets.getSelectionModel().getSelectedItems()));
        gpMenu.add(bPreview, 4, 1);

        Button bStart = new Button("Start");
        bStart.setOnAction(e -> {
            FlashCardsSet selectedSet = tvFlashCardsSets.getSelectionModel().getSelectedItem();
            if(selectedSet.getCount() == 0){
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Empty set");
                alert.setHeaderText(null);
                alert.setContentText("This set is empty!\nDo you want to add new FlashCards?");

                Optional<ButtonType> result = alert.showAndWait();
                if(result.get() == ButtonType.OK){
                    addFlashCard(selectedSet, false);
                }
            }else{
                startSet(selectedSet);
            }
        });
        bStart.disableProperty().bind(Bindings.isEmpty(tvFlashCardsSets.getSelectionModel().getSelectedItems()));
        gpMenu.add(bStart, 5, 1);


        scMenu= new Scene(gpMenu);
        stWindow.setScene(scMenu);
        stWindow.show();

    }

    @Override
    public void stop() throws Exception{
        if(unsavedChanges){
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Warning: Unsaved changes!");
            alert.setHeaderText(null);
            alert.setContentText("Do you want to save unsaved changes?");

            Optional<ButtonType> result = alert.showAndWait();
            if(result.get() == ButtonType.OK){
                saveSets();
            }
        }
        System.out.println("Wychodze!");
        super.stop();
    }

    private void loadSets() throws Exception{
        File fSets = new File("FlashCardsSets");
        if(fSets.exists() && !fSets.isDirectory()){
            FileInputStream fi = new FileInputStream(fSets);
            ObjectInputStream oi = new ObjectInputStream(fi);
            FlashCardsSet input;

            while(true){
                try{
                    input = (FlashCardsSet) oi.readObject();
                    tvFlashCardsSets.getItems().add(input);
                }catch(IOException e){
                    oi.close();
                    fi.close();
                    break;
                }
            }
        }
    }

    private void saveSets(){
        try {
            FileOutputStream fo = new FileOutputStream(new File("FlashCardsSets"));
            ObjectOutputStream oo = new ObjectOutputStream(fo);

            for(FlashCardsSet toSave : tvFlashCardsSets.getItems()){
                oo.writeObject(toSave);
            }
            oo.close();
            fo.close();

            unsavedChanges = false;
            bSave.setDisable(true);
        }catch (IOException ignore){}
    }

    private void addNewSet(){
        boolean[] setAdded = {false};
        FlashCardsSet[] newSet = {null};

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Add new Flash Card set");
        dialog.setHeaderText(null);

        ButtonType bAdd = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(bAdd, ButtonType.CANCEL);

        GridPane gpDialog = new GridPane();

        TextField tfName = new TextField();
        TextField tfDescription = new TextField();

        gpDialog.add(new Label("Name:"), 0, 0);
        gpDialog.add(tfName, 1, 0);
        gpDialog.add(new Label("Description:"), 0, 1);
        gpDialog.add(tfDescription, 1, 1);

        Node addButton = dialog.getDialogPane().lookupButton(bAdd);
        addButton.setDisable(true);

        tfName.textProperty().addListener((observable, oldValue, newValue) -> {
            addButton.setDisable(newValue.trim().isEmpty());
        });

        dialog.getDialogPane().setContent(gpDialog);

        dialog.setResultConverter(dialogButton -> {
            if(dialogButton == bAdd){
                if(tfDescription.getLength() > 0){
                    newSet[0] = new FlashCardsSet(tfName.getText().trim(), tfDescription.getText().trim());
                }else{
                    newSet[0] = new FlashCardsSet(tfName.getText().trim());
                }

                tvFlashCardsSets.getItems().add(newSet[0]);

                setAdded[0] = true;
                unsavedChanges = true;
                bSave.setDisable(false);
            }
            return null;
        });

        dialog.showAndWait();

        if(setAdded[0])
            addFlashCard(newSet[0], false);
    }

    private void addFlashCard(FlashCardsSet setToAdd, boolean inPreview){
        boolean[] addNext = {false};

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Add new Flash Card");
        dialog.setHeaderText(null);

        ButtonType bAdd = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(bAdd, ButtonType.CANCEL);

        GridPane gpDialog = new GridPane();

        TextField tfWord = new TextField();
        TextField tfTranslation = new TextField();
        TextField tfDescription = new TextField();

        gpDialog.add(new Label("Word:"), 0, 0);
        gpDialog.add(tfWord, 1, 0);
        gpDialog.add(new Label("Translation:"), 0, 1);
        gpDialog.add(tfTranslation, 1, 1);
        gpDialog.add(new Label("Description:"), 0, 2);
        gpDialog.add(tfDescription, 1, 2);

        AtomicBoolean tfWordIsEmpty = new AtomicBoolean(true);
        AtomicBoolean tfTranIsEmpty = new AtomicBoolean(true);

        Node addButton = dialog.getDialogPane().lookupButton(bAdd);
        addButton.setDisable(tfWordIsEmpty.get() && tfTranIsEmpty.get());

        tfWord.textProperty().addListener((observable, oldValue, newValue) -> {
            tfWordIsEmpty.set(newValue.trim().isEmpty());
            addButton.setDisable(tfWordIsEmpty.get() || tfTranIsEmpty.get());

        });
        tfTranslation.textProperty().addListener((observable, oldValue, newValue) -> {
            tfTranIsEmpty.set(newValue.trim().isEmpty());
            addButton.setDisable(tfWordIsEmpty.get() || tfTranIsEmpty.get());
        });

        dialog.getDialogPane().setContent(gpDialog);

        dialog.setResultConverter(dialogButton -> {
            if(dialogButton == bAdd){
                FlashCard newCard;
                if(tfDescription.getLength() > 0){
                    newCard = new FlashCard(tfWord.getText().trim(), tfTranslation.getText().trim(), tfDescription.getText().trim());
                }else{
                    newCard = new FlashCard(tfWord.getText().trim(), tfTranslation.getText().trim());
                }

                setToAdd.getFlashCards().add(newCard);
                setToAdd.setCount(setToAdd.getCount()+1);
                if(inPreview)
                    tvFlashCards.getItems().add(newCard);
                if(!inPreview)
                    tvFlashCardsSets.refresh();

                addNext[0] = true;
                unsavedChanges = true;
                bSave.setDisable(false);
            }
            return null;
        });

        dialog.showAndWait();
        if(addNext[0])
            addFlashCard(setToAdd, inPreview);

    }

    private void deleteSet(FlashCardsSet set){
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Flash Cards Set");
        alert.setHeaderText(null);
        alert.setContentText("Do you want to delete this set: "+set.getName()+"?");

        Optional<ButtonType> result = alert.showAndWait();
        if(result.get() == ButtonType.OK){
            tvFlashCardsSets.getItems().remove(set);
            unsavedChanges = true;
            bSave.setDisable(false);
        }
    }

    private void deleteCard(FlashCardsSet set, FlashCard card){
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Flash Cards");
        alert.setHeaderText(null);
        alert.setContentText("Do you want to delete this FlashCard?");

        Optional<ButtonType> result = alert.showAndWait();
        if(result.get() == ButtonType.OK){
            tvFlashCards.getItems().remove(card);
            set.getFlashCards().remove(card);
            set.setCount(set.getCount()-1);
            unsavedChanges = true;
            bSave.setDisable(false);
        }
    }

    private void editSet(FlashCardsSet setToEdit){
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Edit Flash Cards Set");
        dialog.setHeaderText(null);

        ButtonType bAdd = new ButtonType("Edit", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(bAdd, ButtonType.CANCEL);

        GridPane gpDialog = new GridPane();

        TextField tfName = new TextField(setToEdit.getName());
        TextField tfDescription = new TextField(setToEdit.getDescription());

        gpDialog.add(new Label("Name:"), 0, 0);
        gpDialog.add(tfName, 1, 0);
        gpDialog.add(new Label("Description:"), 0, 1);
        gpDialog.add(tfDescription, 1, 1);

        Node addButton = dialog.getDialogPane().lookupButton(bAdd);
        //addButton.setDisable(true); //Set has always name

        tfName.textProperty().addListener((observable, oldValue, newValue) -> {
            addButton.setDisable(newValue.trim().isEmpty());
        });

        dialog.getDialogPane().setContent(gpDialog);

        dialog.setResultConverter(dialogButton -> {
            if(dialogButton == bAdd){
                setToEdit.setName(tfName.getText().trim());
                setToEdit.setDescription(tfDescription.getText().trim());
                unsavedChanges = true;
                bSave.setDisable(false);
                tvFlashCardsSets.refresh();
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void editCard(FlashCard cardToEdit){
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Edit Flash Card");
        dialog.setHeaderText(null);

        ButtonType bEdit = new ButtonType("Edit", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(bEdit, ButtonType.CANCEL);

        GridPane gpDialog = new GridPane();

        TextField tfWord = new TextField(cardToEdit.getWord());
        TextField tfTranslation = new TextField(cardToEdit.getTranslation());
        TextField tfDescription = new TextField(cardToEdit.getDescription());

        gpDialog.add(new Label("Word:"), 0, 0);
        gpDialog.add(tfWord, 1, 0);
        gpDialog.add(new Label("Translation:"), 0, 1);
        gpDialog.add(tfTranslation, 1, 1);
        gpDialog.add(new Label("Description:"), 0, 2);
        gpDialog.add(tfDescription, 1, 2);

        AtomicBoolean tfWordIsEmpty = new AtomicBoolean(false);
        AtomicBoolean tfTranIsEmpty = new AtomicBoolean(false);

        Node editButton = dialog.getDialogPane().lookupButton(bEdit);

        tfWord.textProperty().addListener((observable, oldValue, newValue) -> {
            tfWordIsEmpty.set(tfWord.getText().trim().length() == 0);
            editButton.setDisable(tfWordIsEmpty.get() || tfTranIsEmpty.get());

        });
        tfTranslation.textProperty().addListener((observable, oldValue, newValue) -> {
            tfTranIsEmpty.set(tfTranslation.getText().trim().length() == 0);
            editButton.setDisable(tfWordIsEmpty.get() || tfTranIsEmpty.get());
        });

        dialog.getDialogPane().setContent(gpDialog);

        dialog.setResultConverter(dialogButton -> {
            if(dialogButton == bEdit){
                cardToEdit.setWord(tfWord.getText().trim());
                cardToEdit.setTranslation(tfTranslation.getText().trim());
                if(tfDescription.getText() == null)
                    cardToEdit.setDescription("");
                else
                    cardToEdit.setDescription(tfDescription.getText().trim());
                tvFlashCards.refresh();
                unsavedChanges = true;
                bSave.setDisable(false);
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void previewSet(FlashCardsSet previewSet){
        GridPane gpPreview = new GridPane();
        gpPreview.add(new Label("FlashCardSet name: "), 0, 0);
        Label lName = new Label(previewSet.getName());
        gpPreview.add(lName, 1, 0);
        gpPreview.add(new Label("FlashCardSet count:"), 2, 0);
        Label lCount = new Label(previewSet.getCount()+"");
        gpPreview.add(lCount, 3, 0);

        TableColumn<FlashCard, String> tcWord = new TableColumn<>("Word");
        tcWord.setCellValueFactory(new PropertyValueFactory<>("word"));

        TableColumn<FlashCard, String> tcTranslation = new TableColumn<>("Translation");
        tcTranslation.setCellValueFactory(new PropertyValueFactory<>("translation"));

        TableColumn<FlashCard, String> tcDescription = new TableColumn<>("Description");
        tcDescription.setCellValueFactory(new PropertyValueFactory<>("description"));

        tvFlashCards = new TableView();
        ObservableList<FlashCard> flashCards = FXCollections.observableArrayList();
        flashCards.addAll(previewSet.getFlashCards());
        tvFlashCards.setItems(flashCards);
        tvFlashCards.getColumns().addAll(tcWord, tcTranslation, tcDescription);

        gpPreview.add(tvFlashCards, 0 ,1, 5, 1);

        Button bBack = new Button("Back");
        bBack.setOnAction(e -> {
            stWindow.setScene(scMenu);
            tvFlashCardsSets.refresh();
        });
        gpPreview.add(bBack, 0, 2);

        Button bAdd = new Button("Add");
        bAdd.setOnAction(e -> {
            addFlashCard(previewSet, true);
            lCount.setText(previewSet.getCount()+"");
        });
        gpPreview.add(bAdd, 1, 2);

        Button bDelete = new Button("Delete");
        bDelete.setOnAction(e -> {
            deleteCard(previewSet, tvFlashCards.getSelectionModel().getSelectedItem());
            lCount.setText(previewSet.getCount()+"");
        });
        bDelete.disableProperty().bind(Bindings.isEmpty(tvFlashCards.getSelectionModel().getSelectedItems()));
        gpPreview.add(bDelete, 2, 2);

        Button bEdit = new Button("Edit");
        bEdit.setOnAction(e -> editCard(tvFlashCards.getSelectionModel().getSelectedItem()));
        bEdit.disableProperty().bind(Bindings.isEmpty(tvFlashCards.getSelectionModel().getSelectedItems()));
        gpPreview.add(bEdit, 3, 2);

        scPreview= new Scene(gpPreview);
        stWindow.setScene(scPreview);
    }

    private void startSet(FlashCardsSet setToPlay){
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Choose flash cards amount");
        dialog.setHeaderText(null);

        ButtonType bTen = new ButtonType("10", ButtonBar.ButtonData.OK_DONE);
        ButtonType bTwenty = new ButtonType("20", ButtonBar.ButtonData.OK_DONE);
        ButtonType bAll = new ButtonType("All", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(bTen, bTwenty, bAll, ButtonType.CANCEL);

        GridPane gpDialog = new GridPane();

        gpDialog.add(new Label("You have chosen set "+setToPlay.getName()), 0, 0);
        gpDialog.add(new Label("This set has "+setToPlay.getCount()+" cards"), 0, 1);
        gpDialog.add(new Label("Choose amount of playinh cards."), 0, 2);

        Node tenButton = dialog.getDialogPane().lookupButton(bTen);
        Node twentyButton = dialog.getDialogPane().lookupButton(bTwenty);
        if(setToPlay.getCount() < 10)
            tenButton.setDisable(true);
        if(setToPlay.getCount() < 20)
            twentyButton.setDisable(true);

        dialog.getDialogPane().setContent(gpDialog);

        dialog.setResultConverter(dialogButton -> {
            if(dialogButton == bTen){
                game(setToPlay, selectCards(setToPlay, 10));
            }else if(dialogButton == bTwenty){
                game(setToPlay, selectCards(setToPlay, 20));
            }else if(dialogButton == bAll){
                game(setToPlay, selectCards(setToPlay, setToPlay.getCount()));
            }
            return null;
        });

        dialog.showAndWait();
    }

    private ArrayList<FlashCard> selectCards(FlashCardsSet set, int amount){
        ArrayList<FlashCard> flashCards = new ArrayList<>(set.getFlashCards());
        ArrayList<FlashCard> cardsToPlay = new ArrayList<>();
        Collections.shuffle(flashCards);
        if(flashCards.size() == amount){
            cardsToPlay = flashCards;
        }else{
            for(int i=0; i<amount; i++){
                cardsToPlay.add(flashCards.get(i));
            }
        }
        return cardsToPlay;
    }

    private void game(FlashCardsSet playingSet, ArrayList<FlashCard> flashCards){
        GridPane gpGame = new GridPane();

        AtomicInteger score = new AtomicInteger();
        AtomicInteger current = new AtomicInteger();

        gpGame.add(new Label("Your score: "), 0, 0);
        Label lScore = new Label("0/"+flashCards.size());
        gpGame.add(lScore, 1, 0);

        Label lCurrent = new Label("0/"+flashCards.size());
        gpGame.add(lCurrent, 3, 0);

        Label lPrevious = new Label();
        gpGame.add(lPrevious, 0, 1);

        gpGame.add(new Label("Word: "), 0, 2);
        Label lWord = new Label(flashCards.get(current.get()).getWord());
        gpGame.add(lWord, 1, 2);

        gpGame.add(new Label("Translation: "), 0, 3);
        TextField tfTranslation = new TextField();
        gpGame.add(tfTranslation, 1, 3);

        Label lHint = new Label();
        gpGame.add(lHint, 2, 3);

        gpGame.add(new Label("Description: "), 0, 4);
        Label lDescription = new Label(flashCards.get(current.get()).getDescription());
        gpGame.add(lDescription, 1, 4);

        AtomicInteger hints = new AtomicInteger(2);
        Button bHint = new Button("Hint");
        bHint.setOnAction(e -> {
            if(hints.get() == 2){
                lHint.setText("Hints: This word have "+flashCards.get(current.get()).getTranslation().length()+" letters.");
                hints.getAndDecrement();
            }else{
                ArrayList<Character> letters = new ArrayList<>();
                for(char c : flashCards.get(current.get()).getTranslation().toCharArray()){
                    letters.add(c);
                }
                Collections.shuffle(letters);
                String hint = "";
                for(char c : letters){
                    hint += c+" ";
                }
                lHint.setText(lHint.getText()+"; Use these letters: "+hint);
                bHint.setDisable(true);
            }
        });
        gpGame.add(bHint, 0, 5);

        Button bConfirm = new Button("Confirm");
        bConfirm.setOnAction(e -> {
            if(tfTranslation.getText().toLowerCase().equals(flashCards.get(current.get()).getTranslation())){
                score.getAndIncrement();
                lScore.setText(score.get()+"/"+flashCards.size());
                lPrevious.setText("Correct!");
            }else{
                lPrevious.setText("Wrong, correct answer was `"+flashCards.get(current.get()).getTranslation()+"`");
            }
            current.getAndIncrement();
            lCurrent.setText(current.get()+"/"+flashCards.size());
            if(current.get()<10){
                lWord.setText(flashCards.get(current.get()).getWord());
                lDescription.setText(flashCards.get(current.get()).getDescription());
                tfTranslation.setText("");
                lHint.setText("");
                bHint.setDisable(false);
                hints.set(2);
            }else{
                bConfirm.setDisable(true);
                bHint.setDisable(true);
            }
        });
        gpGame.add(bConfirm, 1, 5);


        Button bBack = new Button("Back");
        bBack.setOnAction(e -> stWindow.setScene(scMenu));
        //Ask if want to finish game
        gpGame.add(bBack, 0, 6);

        scGame= new Scene(gpGame);
        stWindow.setScene(scGame);
    }
}
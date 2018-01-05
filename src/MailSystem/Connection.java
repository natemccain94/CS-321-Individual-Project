package MailSystem;

/**
   Connects a phone to the mail system. The purpose of this
   class is to keep track of the state of a connection, since
   the phone itself is just a source of individual key presses.
*/
public class Connection
{
   /**
      Construct a Connection object.
      @param s a MailSystem object
      @param p a Telephone object
   */
   public Connection(MailSystem s, Telephone p)
   {
      system = s;
      phone = p;
      resetConnection();
   }

   /**
      Respond to the user's pressing a key on the phone touchpad.
      Tells the connection where to go based on the current state.
      @param key the phone key pressed by the user
   */
   public void dial(String key)
   {       
       if (state == CONNECTED)
         connect(key);
      else if (state == RECORDING)
         login(key);
      else if (state == CHANGE_PASSCODE)
         changePasscode(key);
      else if (state == CHANGE_GREETING)
         changeGreeting(key);
      else if (state == MAILBOX_MENU)
         mailboxMenu(key);
      else if (state == MESSAGE_MENU)
         messageMenu(key);
      // The forward message area.
      else if (state == FORWARD_MESSAGE)
          forwardMessage(key);
      // The broadcast message area.
      else if (state == BROADCAST_ALL)
          login(key);
   }

   /**
      Record voice.
      @param voice voice spoken by the user
   */
   public void record(String voice)
   {
      if (state == RECORDING || state == CHANGE_GREETING || state == BROADCAST_ALL)
         currentRecording += voice;
   }
   
   
   /**
      The user hangs up the phone.
      If the state is set to RECORDING, the current recording will be
      stored as a new message in the current mailbox. If the state is
      set to BROADCAST_ALL, the current recording will be stored as a
      new message in all mailboxes in the mail system. However, if the
      current message is empty (no message), no message will be added
      (for both RECORDING and BROADCAST_ALL states).
   */
   public void hangup()
   {
      // If the currentRecording is not empty and state == RECORDING
      if (state == RECORDING && !currentRecording.equals(""))
         currentMailbox.addMessage(new Message(currentRecording));
      // If the currentRecording is not empty and state == BROADCAST_ALL
      else if (state == BROADCAST_ALL && !currentRecording.equals(""))
      {
          // Counter for reaching all mailboxes
          int i = 1;
          // Flag for indicating if all mailboxes have been reached
          boolean reachAllMailboxes = false;
          // While all mailboxes have not been reached
          while (reachAllMailboxes == false)
          {
              // Conversion to match need of MailSystem function call
              String readIntI = Integer.toString(i);
              // Set the current mailbox
              currentMailbox = system.findMailbox(readIntI);
              // If there exists a mailbox at this location
              if (currentMailbox != null)
              {
                  // Add the message to the mailbox
                  currentMailbox.addMessage(new Message(currentRecording));
                  i++; // Counter to reach next mailbox
              }
              // No mailbox exists at this location
              else
              {
                  reachAllMailboxes = true; // All mailboxes are found
              }
          }
      }
      resetConnection();
   }

   /**
      Reset the connection to the initial state and prompt
      for mailbox number
   */
   private void resetConnection()
   {
      currentRecording = "";
      accumulatedKeys = "";
      state = CONNECTED;
      phone.speak(INITIAL_PROMPT);
   }

   /**
      Try to connect the user with the specified mailbox. If the user
      dials "00#", the connection state will be set to BROADCAST_ALL.
      @param key the phone key pressed by the user
   */
   private void connect(String key)
   {
      if (key.equals("#"))
      {
         currentMailbox = system.findMailbox(accumulatedKeys);
         if (accumulatedKeys.equals("00"))
         {
             state = BROADCAST_ALL;
             phone.speak(BROADCAST_ALL_PROMPT);
         }
         else if (currentMailbox != null)
         {
             state = RECORDING;
             phone.speak(currentMailbox.getGreeting());
             accumulatedKeys = "";
         }
         else
         {
             phone.speak("Incorrect mailbox number. Try again!");
             accumulatedKeys = "";
         }
      }
      else
         accumulatedKeys += key;
        
   }

   /**
      Try to log in the user. If the user dials '#' and the state is set
      to BROADCAST_ALL, an error message will be displayed and the '#' 
      key will be discarded. If the state is not set to RECORDING, the
      user can either log into the current mailbox, or leave a message.
      @param key the phone key pressed by the user
   */
   private void login(String key)
   {
       // If the user is trying to log in
      if (key.equals("#") && state != BROADCAST_ALL)
      {
          // If the passcode entered is correct
         if (currentMailbox.checkPasscode(accumulatedKeys))
         {
             // Go to the mailbox's menu
            state = MAILBOX_MENU;
            phone.speak(MAILBOX_MENU_TEXT);
         }
         // The passcode was incorrect.
         else
            phone.speak("Incorrect passcode. Try again!");
         accumulatedKeys = "";
      }
      // The user enters '#' while state is BROADCAST_ALL (attempted login)
      else if (key.equals("#") && state == BROADCAST_ALL)
      {
          String output = "You entered a bad character. I will ignore your \n";
          output += "mistake. Please continue with your message. Please \n";
          output += "hang up when you are finished.";
          phone.speak(output);
      }
      else
         accumulatedKeys += key;
   }

   /**
      Change passcode.
      @param key the phone key pressed by the user
   */
   private void changePasscode(String key)
   {
      if (key.equals("#"))
      {
         currentMailbox.setPasscode(accumulatedKeys);
         state = MAILBOX_MENU;
         phone.speak(MAILBOX_MENU_TEXT);
         accumulatedKeys = "";
      }
      else
         accumulatedKeys += key;
   }

   /**
      Change greeting.
      @param key the phone key pressed by the user
   */
   private void changeGreeting(String key)
   {
      if (key.equals("#"))
      {
         currentMailbox.setGreeting(currentRecording);
         currentRecording = "";
         state = MAILBOX_MENU;
         phone.speak(MAILBOX_MENU_TEXT);
      }
   }
   
   /**
     Forward message. The user dials the extension of the mailbox
     they wish to forward the current message to. If the user 
     wishes to return to the Message Menu without forwarding a
     message, they must enter "0#".
     @param key the phone key pressed by the user
   */
   private void forwardMessage(String key)
   {
       Message fwdMessage = currentMailbox.getCurrentMessage();
       if (key.equals("#")){
           Mailbox forwardingMailbox = system.findMailbox(accumulatedKeys);
           // The accumulated keys matches a mailbox
           if (forwardingMailbox != null){
               // Pass the message on to the mailbox
               forwardingMailbox.addMessage(fwdMessage);
               state = MESSAGE_MENU;
               phone.speak(MESSAGE_MENU_TEXT);
           }
           // If the user wishes to return to the message menu
           else if (accumulatedKeys.equals("0"))
           {
               state = MESSAGE_MENU;
               phone.speak(MESSAGE_MENU_TEXT);
               accumulatedKeys = "";
           }
           // An incorrect mailbox number has been entered.
           else{
               phone.speak("Incorrect mailbox number. \n");
               phone.speak(FORWARD_MESSAGE_PROMPT);
               accumulatedKeys = "";
           }
       }
       else{
           accumulatedKeys += key;
       }
   }

   /**
      Respond to the user's selection from mailbox menu.
      @param key the phone key pressed by the user
   */
   private void mailboxMenu(String key)
   {
      if (key.equals("1"))
      {
         state = MESSAGE_MENU;
         phone.speak(MESSAGE_MENU_TEXT);
      }
      else if (key.equals("2"))
      {
         state = CHANGE_PASSCODE;
         phone.speak("Enter new passcode followed by the # key");
      }
      else if (key.equals("3"))
      {
         state = CHANGE_GREETING;
         phone.speak("Record your greeting, then press the # key");
      }
   }

   /**
      Respond to the user's selection from message menu.
      @param key the phone key pressed by the user
   */
   private void messageMenu(String key)
   {
       // Listen to current message
      if (key.equals("1"))
      {
         String output = "";
         Message m = currentMailbox.getCurrentMessage();
         if (m == null) output += "No messages." + "\n";
         else output += m.getText() + "\n";
         output += MESSAGE_MENU_TEXT;
         phone.speak(output);
      }
      // Save current message
      else if (key.equals("2"))
      {
         currentMailbox.saveCurrentMessage();
         phone.speak(MESSAGE_MENU_TEXT);
      }
      // Delete current message
      else if (key.equals("3"))
      {
         currentMailbox.removeCurrentMessage();
         phone.speak(MESSAGE_MENU_TEXT);
      }
      // Forward current message
      else if (key.equals("4")){
          // Make sure there is a message to forward
          String output = "";
          Message fwdMessage = currentMailbox.getCurrentMessage();
          // If there are no messages, output error message.
          if (fwdMessage == null){
              output += "No messages to forward." + "\n";
              output += MESSAGE_MENU_TEXT;
              phone.speak(output);
          }
          // There is a message to forward
          else {
              phone.speak(FORWARD_MESSAGE_PROMPT);
              state = FORWARD_MESSAGE;
          }  
      }
      // Return to mailbox menu
      else if (key.equals("5"))
      {
         state = MAILBOX_MENU;
         phone.speak(MAILBOX_MENU_TEXT);
      }
      
   }
   
   

   private MailSystem system;
   private Mailbox currentMailbox;
   private String currentRecording;
   private String accumulatedKeys;
   private Telephone phone;
   private int state;

   private static final int DISCONNECTED = 0;
   private static final int CONNECTED = 1;
   private static final int RECORDING = 2;
   private static final int MAILBOX_MENU = 3;
   private static final int MESSAGE_MENU = 4;
   private static final int CHANGE_PASSCODE = 5;
   private static final int CHANGE_GREETING = 6;
   // Forward message state
   private static final int FORWARD_MESSAGE = 7;
   // Broadcast to all mailboxes state
   private static final int BROADCAST_ALL = 8;

   // Updated prompt to reflect new operation.
   private static final String INITIAL_PROMPT = 
         "Enter mailbox number followed by #. If you would like to \n"
           + "broadcast a message to all mailboxes, please enter \n"
           + "00 followed by #. \n";
   private static final String MAILBOX_MENU_TEXT = 
         "Enter 1 to listen to your messages\n"
         + "Enter 2 to change your passcode\n"
         + "Enter 3 to change your greeting";
   // Updated text to reflect new operation.
   private static final String MESSAGE_MENU_TEXT = 
         "Enter 1 to listen to the current message\n"
         + "Enter 2 to save the current message\n"
         + "Enter 3 to delete the current message\n"
         + "Enter 4 to forward the current message to a specified mailbox\n"  
         + "Enter 5 to return to the mailbox menu";
   
   // Prompt for forwarding a message
   private static final String FORWARD_MESSAGE_PROMPT =
           "Dial the mailbox number that you would like\n"
           + "to forward the message to follow by #. If you would like\n"
           + "to return to the Message Menu, please dial 0 followed\n"
           + "by #.";
   // Prompt for broadcasting a message to all mailboxes.
   private static final String BROADCAST_ALL_PROMPT =
        "Please leave a message for all mailbox users. \n"
           + "When you are finished recording, please hang up.";
   
}












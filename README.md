# Tactosy-ble

Tactosy-ble is a library for networking with `tactosy`, handset for VR.
By extending our `client` and `service`, you can easily connect with our
tactosy devices and send/receive data.


# Requirements

You should own tactosy or BLE-supported devices.

In case of other devices, you should know their own characteristic.


# Installation

* gradle

```groovy
compile 'com.bhaptics.tactosy:ble:0.10.1'
```

* or maven

```xml
<dependency>
  <groupId>com.bhaptics.tactosy</groupId>
  <artifactId>ble</artifactId>
  <version>0.10.1</version>
  <type>pom</type>
</dependency>
```

# How to use

```
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    Button on, off;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        HapticPlayer.getInstance(this).bindService(this);
        on = findViewById(R.id.button_on);
        off = findViewById(R.id.button_off);

        on.setOnClickListener(this);
        off.setOnClickListener(this);


    }

    @Override
    public void onClick(View view) {
        Button button = (Button) view;
        HapticPlayer player = HapticPlayer.getInstance(getApplicationContext());
        if ("On".equals(button.getText())) {
            List<Feedback> feedbacks = new ArrayList<>();
            Feedback feedback = new Feedback();
            feedback.mPosition = "Left";
            feedback.mValues = new byte[20];
            feedback.mValues[5] = 100;
            feedbacks.add(feedback);
            player.submit(feedbacks);
        } else {
            player.turnOff();
        }
    }
}
```

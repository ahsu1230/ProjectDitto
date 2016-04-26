# ProjectDitto

ProjectDitto is a prototype of a potential future feature in mobile apps: "App Streaming".
This means giving users the ability to use apps that are not currently on their phone.
When using a service, a user can use the native app to access content, but only if they have it installed on their phone.
If they do not however, they can access that content through a mobile website (if available).
Now imagine users can have the ability to use/consume content in the app format/layout, without having to download or install the app onto their phone.

# How it works
As a prototype, we will be using two phones.
Let's say we have one phone device, HOST which has some app A.
We have another phone device, IMPOSTER, which does not have app A.

IMPOSTER contains an app with an ImposterActivity. 
ImposterActivity sends requests to MIMIC whatever screen is on HOST.

HOST receives these MIMIC requests and streams a file back as a response.
This response file is simply the layout for the current screen of app A that HOST is displaying.

When IMPOSTER receives the entire file, 
ImposterActivity will re-render the layout file and display it.

From there, ImposterActivity will detect any gesture interactions by the user.
The interaction (click, scroll, swipe, rotate, etc.) will be recorded and set to HOST.
HOST will apply that interaction on top of the HOST activity (like pressing buttons, changing screens, rotating, etc.)

After that, IMPOSTER will send another MIMIC request to HOST, receive another file through stream
and re-render that layout (this layout will have the applied interaction).

In other words, ImposterActivity is an Android activity that can transform into any other activity
if given enough information. It can copy layouts and mimic interactions - just like the Transformer Pokemon: Ditto.

# Creating ProjectDitto
ProjectDitto was first inspired by Google's App Streaming feature.
Google runs virtual machines to emulate apps (that they have indexed) and has a small list of apps that support this "App Streaming" feature.
You can read more about it here: http://marketingland.com/google-app-streaming-web-of-apps-152449

I wanted to create something like that for Nextbit's company-wide Spring Hackathon 2016.
But since I don't have virtual machines, I decided to use another phone with a simple app.
The experience was extremely fun, but I ran into a lot of complications including
writing my own layout XML parser, writing my own LayoutInflater, and handling sockets for phone-to-phone connection.
The XML parsing assumes resources are pre-compiled, not external files and Android's LayoutInflater is very confusing
to use with custom XML parsers.

# Future Work
 - Improve the Custom XMLParser and LayoutInflaters
 - Somehow detect how mimicking Fragments would work
 - Use a cloud emulator instead of another phone for better practicality.
package com.ogm.market.util;

import com.ogm.market.model.NearbyLocation;
import com.ogm.market.model.Property;
import com.ogm.market.repository.PropertyRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class DataInitializer implements CommandLineRunner {

    private final PropertyRepository repo;
    //    private static final String BASE_URL = "https://ogm-backend-clean-879813720468.asia-south1.run.app";
    private static final String BASE_URL = "http://localhost:8080";

    public DataInitializer(PropertyRepository repo) {
        this.repo = repo;
    }

    private String slugify(String text) {
        return text.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }


    @Override
    public void run(String... args) {


// ------------------------- PROPERTY 1 -------------------------

        Property p1 = Property.builder()
                .title("Singapore Style 4 BHK Villa in Gattahalli")
                .location("Astro GreenCascade, Gattahalli, Bengaluru")
                .slug(slugify("astro-green-cascade-off-sarjapur-road"))
                .price("₹ 6.32 Cr")
                .type("Residential Villa")
                .sqft("4300")
                .reraApproved(true)
                .brochureFile("Astro Green Cascade - Life style Experience Brochure.pdf")
                .image(BASE_URL + "/images/p1/property-01-main-01.jpg")
                .mainImages(Arrays.asList(
                        BASE_URL + "/images/p1/property-01-main-01.jpg",
                        BASE_URL + "/images/p1/property-01-main-01.jpg"
                ))
                .images(Arrays.asList(
                        BASE_URL + "/images/p1/property-01-gallery-01.png",
                        BASE_URL + "/images/p1/property-01-gallery-02.png",
                        BASE_URL + "/images/p1/property-01-gallery-03.png",
                        BASE_URL + "/images/p1/property-01-gallery-04.png",
                        BASE_URL + "/images/p1/property-01-gallery-05.png",
                        BASE_URL + "/images/p1/property-01-gallery-06.png",
                        BASE_URL + "/images/p1/property-01-gallery-07.png",
                        BASE_URL + "/images/p1/property-01-gallery-08.jpg",
                        BASE_URL + "/images/p1/property-01-gallery-09.png",
                        BASE_URL + "/images/p1/property-01-gallery-10.jpg",
                        BASE_URL + "/images/p1/property-01-gallery-11.jpg",
                        BASE_URL + "/images/p1/property-01-gallery-12.jpg",
                        BASE_URL + "/images/p1/property-01-gallery-13.jpg",
                        BASE_URL + "/images/p1/property-01-gallery-14.jpg",
                        BASE_URL + "/images/p1/property-01-gallery-15.jpg",
                        BASE_URL + "/images/p1/property-01-gallery-16.jpg",
                        BASE_URL + "/images/p1/property-01-gallery-17.jpg",
                        BASE_URL + "/images/p1/property-01-gallery-18.jpg"
                ))
                .landArea("2400 sq.ft - ")
                .maintenance("N/A")
                .bedrooms("4 -")
                .bathrooms("4 -")
                .builtupArea("4300 sq.ft")
                .parking("3 Cars")
                .furnishing("Unfurnished")
                .facing("East")
                .description("A home with walls is just a home, but a home full of life and experience is a castle. Project Chief represents character; it gives a breathtaking luxury life with a uniquely styled exterior build and interior mastery of class. With 14mm-thick laminated glass doors covering the outer walls, in continuation with the foldable windows beneath them, it’s a place wrapped in glass of class. From clay cladding walls to Italian marble floors and an extravagant staircase, it has the true personality of a king and the heart of a queen.")
                .videoUrl(BASE_URL + "/images/p1/property-01-video-01.mp4")
                .amenities(Arrays.asList(
                        "Car Parking (2 car parking slots)",
                        "Indoor Garden / Courtyard",
                        "Dining Area",
                        "Kitchen",
                        "Utility Area",
                        "Powder Room / Toilet",
                        "Indoor Lift (5-person lift)",
                        "Spiral Staircase",
                        "Living Room / Family Lounge",
                        "Indoor Garden / Open-to-sky area",
                        "Media Room / Entertainment Zone",
                        "Pooja Room",
                        "Large Terraces Sit-out Area"
                ))
                .nearby(Arrays.asList(
                        new NearbyLocation(
                                "Play Arena",
                                "4.8 km",
                                "Play",
                                BASE_URL + "/images/p1/play-arena.png"
                        ),
                        new NearbyLocation(
                                "Bier Library ",
                                "4.6 km",
                                "Restaurant",
                                BASE_URL + "/images/p1/bier-library.jpeg"
                        ),
                        new NearbyLocation(
                                "Gladia",
                                "5.4 km",
                                "Restaurant",
                                BASE_URL + "/images/p1/gladia.png"
                        )
                ))
                .build();


// ------------------------- PROPERTY 2 -------------------------

        Property p2 = Property.builder()
                .title("2 & 3 BHK Flats in Kasavanahalli")
                .slug(slugify("2 & 3 BHK Flats in Kasavanahalli"))
                .location("Kasavanhalli , Bengaluru, ( Near hsr layout )")
                .price("₹1.25 Cr")
                .type("Residential Building")
                .sqft("1569")
                .reraApproved(true)
                .brochureFile("property-2.pdf")
                .image(BASE_URL + "/images/p2/property-02-main-01.jpeg")
                .mainImages(Arrays.asList(
                        BASE_URL + "/images/p2/property-02-main-01.jpeg",
                        BASE_URL + "/images/p2/property-02-main-01.jpeg",
                        BASE_URL + "/images/p2/property-02-main-01.jpeg"
                ))
                .images(Arrays.asList(
                        BASE_URL + "/images/p2/property-02-gallery-01.jpeg",
                        BASE_URL + "/images/p2/property-02-gallery-02.jpeg",
                        BASE_URL + "/images/p2/property-02-gallery-03.jpeg",
                        BASE_URL + "/images/p2/property-02-gallery-04.jpeg",
                        BASE_URL + "/images/p2/property-02-gallery-05.jpeg",
                        BASE_URL + "/images/p2/property-02-gallery-06.jpeg",
                        BASE_URL + "/images/p2/property-02-gallery-07.jpeg",
                        BASE_URL + "/images/p2/property-02-gallery-08.jpeg",
                        BASE_URL + "/images/p2/property-02-gallery-09.jpeg",
                        BASE_URL + "/images/p2/property-02-gallery-10.jpeg",
                        BASE_URL + "/images/p2/property-02-gallery-11.jpeg",
                        BASE_URL + "/images/p2/property-02-gallery-12.jpeg"
                ))
                .bedrooms("5")
                .bathrooms("5")
                .carpetArea("N/A")
                .builtupArea("1569 sq.ft")
                .parking("Ground Floor Car Parking")
                .maintenance("N/A")
                .furnishing("Un-Furnished")
                .facing("East")
                .description("Living in this residence feels effortlessly balanced, with well-planned 2BHK and 3BHK homes arranged around generous passages and open internal courtyards that bring light into everyday life. Each apartment offers a sense of privacy while still feeling connected to the rhythm of a thoughtfully designed community. The experience is calm, convenient, and quietly elevated—perfect for a lifestyle that values both comfort and space.")
                .videoUrl(BASE_URL + "/videos/p2_house_tour.mp4")
                .amenities(Arrays.asList(
                        "Kids Play Area",
                        "Club House/Party Hall",
                        "Gymnasium"
                ))
                .build();


// ------------------------- PROPERTY 3 -------------------------

        Property p3 = Property.builder()
                .title("2 BHK Flats in Junnasandra")
                .slug(slugify("2 BHK Flats in Junnasandra"))
                .location("Junnasandra, Near Old Wipro Gate, Bengaluru")
                .price("₹40 Lakhs")
                .type("Residential Building")
                .sqft("1100")
                .reraApproved(true)
                .soldOut(true)
                .image(BASE_URL + "/images/p3/property-03-main-01.jpg")
                .mainImages(Arrays.asList(
                        BASE_URL + "/images/p3/property-03-main-01.jpg",
                        BASE_URL + "/images/p3/property-03-main-01.jpg"
                ))
                .images(Arrays.asList(
                        BASE_URL + "/images/p3/property-03-gallery-01.jpg",
                        BASE_URL + "/images/p3/property-03-gallery-02.jpg",
                        BASE_URL + "/images/p3/property-03-gallery-03.jpg",
                        BASE_URL + "/images/p3/property-03-gallery-04.jpg",
                        BASE_URL + "/images/p3/property-03-gallery-05.jpg",
                        BASE_URL + "/images/p3/property-03-gallery-06.jpg",
                        BASE_URL + "/images/p3/property-03-gallery-07.jpg",
                        BASE_URL + "/images/p3/property-03-gallery-08.jpg",
                        BASE_URL + "/images/p3/property-03-gallery-09.jpg",
                        BASE_URL + "/images/p3/property-03-gallery-10.jpg",
                        BASE_URL + "/images/p3/property-03-gallery-11.jpg"
                ))
                .bedrooms("3")
                .bathrooms("3")
                .carpetArea("1100 sq.ft")
                .builtupArea("1100 sq.ft")
                .parking("2 Cars")
                .maintenance("N/A")
                .furnishing("Unfurnished")
                .facing("East & North")
                .description("Living in this residence feels effortlessly balanced, with well-planned 2BHK and 3BHK homes arranged around generous passages and open internal courtyards that bring light into everyday life. Each apartment offers a sense of privacy while still feeling connected to the rhythm of a thoughtfully designed community. The experience is calm, convenient, and quietly elevated—perfect for a lifestyle that values both comfort and space.")
                .videoUrl(BASE_URL + "/videos/apartment_tour.mp4")
                .amenities(Arrays.asList(
                        "Kids Play Area",
                        "Club House/Party Hall",
                        "Gymnasium"
                ))
                .build();
// ------------------------- PROPERTY 4 -------------------------

        Property p4 = Property.builder()
                .title("2 BHK in Sobha Dream Acres ")
                .slug(slugify("2 BHK in Sobha Dream Acres "))
                .location("Sobha Dream Acres, Varthur, Bengaluru")
                .price("₹75.5 Lakhs")
                .type("Large Community Building")
                .sqft("1012")
                .reraApproved(true)
                .soldOut(true)
                .image(BASE_URL + "/images/p4/property-04-main-01.jpg")
                .mainImages(Arrays.asList(
                        BASE_URL + "/images/p4/property-04-main-01.jpg",
                        BASE_URL + "/images/p4/property-04-main-01.jpg"
                ))
                .images(Arrays.asList(
                        BASE_URL + "/images/p4/property-04-gallery-01.jpg",
                        BASE_URL + "/images/p4/property-04-gallery-02.jpg",
                        BASE_URL + "/images/p4/property-04-gallery-03.jpg",
                        BASE_URL + "/images/p4/property-04-gallery-04.jpg",
                        BASE_URL + "/images/p4/property-04-gallery-05.jpg",
                        BASE_URL + "/images/p4/property-04-gallery-06.jpg",
                        BASE_URL + "/images/p4/property-04-gallery-07.jpg",
                        BASE_URL + "/images/p4/property-04-gallery-08.jpg"
                ))
                .bedrooms("2")
                .bathrooms("2")
                .carpetArea("N/A")
                .builtupArea("N/A")
                .parking("N/A")
                .maintenance("N/A")
                .furnishing("Furnished")
                .facing("East")
                .description("Living in a 2 BHK flat at Sobha Dream Acres in Bengaluru means waking up each day to lush green surroundings, wide open spaces and well-maintained landscaping that give a refreshing, calm vibe to everyday life. With access to a rich set of amenities — swimming pools, multiple clubhouses, gym, sports courts, jogging and cycling tracks, children’s play areas, guest rooms and more — there’s always something for relaxation, fitness or social time right at your doorstep. All this, combined with good connectivity to the city and thoughtfully designed flats, makes living here feel comfortable, convenient and community-oriented.")
                .videoUrl(BASE_URL + "/videos/plot_tour.mp4")
                .amenities(Arrays.asList(
                        "Club House",
                        "Swimming Pool",
                        "Kids Play Area",
                        "Tennis Court",
                        "Volleyball Court",
                        "Basketball Court",
                        "Gym",
                        "Co-working Space",
                        "Supermarket",
                        "Guest Rooms",
                        "Bowling Alley",
                        "Yoga",
                        "Billiards",
                        "Cycle Track",
                        "Badminton Court",
                        "Jogging Track",
                        "Landscaped Garden",
                        "Spa & Sauna",
                        "Party Hall",
                        "Outdoor Sports"
                ))
                .build();


// ------------------------- PROPERTY 5 -------------------------

        Property p5 = Property.builder()
                .title("Hello Bali Homes – Holiday Homes")
                .slug(slugify("Hello-Bali-Homes–Holiday-Home-Tamilnadu"))
                .location("Palacode, Nariyanahalli, Tamil Nadu – 636808")
                .price("1.25 Cr to 2.5 Cr")
                .type("2 & 3 BHK Weekend Villas")
                .sqft("620")
                .reraApproved(false)
                .image(BASE_URL + "/images/p5/property-05-main-01.png")
                .mainImages(Arrays.asList(
                        BASE_URL + "/images/p5/property-05-main-01.png",
                        BASE_URL + "/images/p5/property-05-main-01.png"
                ))
                .images(Arrays.asList(
                        BASE_URL + "/images/p5/property-05-gallery-01.png",
                        BASE_URL + "/images/p5/property-05-gallery-02.png",
                        BASE_URL + "/images/p5/property-05-gallery-03.png",
                        BASE_URL + "/images/p5/property-05-gallery-04.png",
                        BASE_URL + "/images/p5/property-05-gallery-05.png"
                ))
                .bedrooms("2")
                .bathrooms("1")
                .carpetArea("480 sq.ft")
                .builtupArea("10k sqft to 20k sqft plots")
                .parking("1 Car")
                .maintenance("NA")
                .furnishing("Unfurnished")
                .facing("East")
                .description("Hello Bali is a thoughtfully designed luxury weekend villa community inspired by Balinese architecture and tropical living. Set amidst lush greenery and tranquil landscapes, the project blends modern European design, nature-centric planning, and resort-style amenities to create a private retreat for families and investors.")
                .videoUrl(BASE_URL + "/videos/studio_tour.mp4")
                .amenities(Arrays.asList(
                        "Swimming Pool",
                        "Gym",
                        "Spa Room (Steam & Sauna)",
                        "Indoor Games & Card Room",
                        "Banquet Hall",
                        "Cloud Kitchen / Café",
                        "Senior Citizens’ Area",
                        "Children’s Play Area",
                        "Jogging & Cycling Track",
                        "Yoga Deck",
                        "Forest / Mango Trail",
                        "Open Badminton Area",
                        "Beach Volleyball Court",
                        "Outdoor Pickleball Court",
                        "Cricket Practice Net",
                        "Landscaped Common Amenity Zones"
                ))
                .build();


// ------------------------- PROPERTY 6 -------------------------

        Property p6 = Property.builder()
                .title("2, 3 & 4 BHK’s Off Sarjapura – Attibele Road")
                .slug(slugify("2, 3 & 4 BHK’s Off Sarjapura – Attibele Road"))
                .location("Maruti Akrida , Bidaraguppe, Mallenahalli, Bangalore, Karnataka 562107")
                .price("₹81 Lakh to ₹1.98 Crore")
                .type("Residential Apartment")
                .sqft("5200")
                .reraApproved(true)
                .image(BASE_URL + "/images/p6/property-06-main-01.jpeg")
                .mainImages(Arrays.asList(
                        BASE_URL + "/images/p6/property-06-main-01.jpeg",
                        BASE_URL + "/images/p6/property-06-main-01.jpeg"
                ))
                .images(Arrays.asList(
                        BASE_URL + "/images/p6/property-06-gallery-01.jpeg",
                        BASE_URL + "/images/p6/property-06-gallery-02.jpeg",
                        BASE_URL + "/images/p6/property-06-gallery-03.jpeg",
                        BASE_URL + "/images/p6/property-06-gallery-04.jpeg"
                ))
                .bedrooms("5")
                .bathrooms("6")
                .carpetArea("3800 sq.ft")
                .builtupArea("1098 – 2390 sq.ft")
                .parking("3 Cars")
                .maintenance("₹12,000")
                .furnishing("Unfurnished")
                .facing("Not Specified")
                .description("Life at Maruti Akrida is designed around the little moments that make a house feel like home. Open mornings, relaxed evenings, and weekends spent without rushing anywhere. Nestled near Sarjapura–Attibele, this RERA-certified residential project offers spacious 2, 3, and 4 BHK apartments across three G+6 blocks, each planned for comfort, light, and flow. The homes are practical yet welcoming, with layouts that adapt effortlessly to everyday family life. Step outside and you’ll find thoughtfully curated amenities including a swimming pool, sports courts, landscaped gardens, and dedicated spaces for children, seniors, and pets. Well-connected to major IT hubs and upcoming infrastructure, Maruti Akrida brings together convenience, community, and a calm, well-balanced lifestyle.")
                .videoUrl(BASE_URL + "/videos/plot_6.mov")
                .amenities(Arrays.asList(
                        "Gated Community with 24×7 Security",
                        "CCTV Surveillance",
                        "Clubhouse & Party Hall",
                        "Gym",
                        "Indoor Games",
                        "Children’s Play Zone",
                        "Swimming Pool",
                        "Multipurpose Sports Court",
                        "Landscaped Gardens & Walking Track"
                )).build();

        // ------------------------- PROPERTY 7 -------------------------

        Property p7 = Property.builder()
                .title("Ridgewood Villas on Sarjapur – Bagalur Road")
                .slug(slugify("Ridgewood Villas on Sarjapur – Bagalur Road"))
                .location("Bagalur - Sarjapur Rd, Hosur, Tamil Nadu 635103")
                .price("₹1.5 Cr – ₹2.0 Cr")
                .type("Independent Villa")
                .sqft("1760")
                .reraApproved(false)
                .image(BASE_URL + "/images/p7/property-07-main-01.jpeg")
                .mainImages(Arrays.asList(
                        BASE_URL + "/images/p7/property-07-main-01.jpeg",
                        BASE_URL + "/images/p7/property-07-main-01.jpeg"
                ))
                .images(Arrays.asList(
                        BASE_URL + "/images/p7/property-07-gallery-01.jpeg",
                        BASE_URL + "/images/p7/property-07-gallery-02.jpeg",
                        BASE_URL + "/images/p7/property-07-gallery-03.jpeg",
                        BASE_URL + "/images/p7/property-07-gallery-04.jpeg",
                        BASE_URL + "/images/p7/property-07-gallery-05.jpeg",
                        BASE_URL + "/images/p7/property-07-gallery-06.jpeg",
                        BASE_URL + "/images/p7/property-07-gallery-07.jpeg",
                        BASE_URL + "/images/p7/property-07-gallery-08.jpeg",
                        BASE_URL + "/images/p7/property-07-gallery-09.jpeg",
                        BASE_URL + "/images/p7/property-07-gallery-10.jpeg",
                        BASE_URL + "/images/p7/property-07-gallery-11.jpeg"
                ))
                .bedrooms("3")
                .bathrooms("3")
                .carpetArea("1280 sq.ft")
                .builtupArea("1500 – 3000 sq.ft")
                .parking("2 Cars")
                .reraApproved(true)
                .maintenance("N/A")
                .furnishing("Unfurnished")
                .facing("Not specified")
                .description("Life at Maruti Akrida is designed around the little moments that make a house feel like home. Open mornings, relaxed evenings, and weekends spent without rushing anywhere. Nestled near Sarjapura–Attibele, this RERA-certified residential project offers spacious 2, 3, and 4 BHK apartments across three G+6 blocks, each planned for comfort, light, and flow. The homes are practical yet welcoming, with layouts that adapt effortlessly to everyday family life. Step outside and you’ll find thoughtfully curated amenities including a swimming pool, sports courts, landscaped gardens, and dedicated spaces for children, seniors, and pets. Well-connected to major IT hubs and upcoming infrastructure, Maruti Akrida brings together convenience, community, and a calm, well-balanced lifestyle.")
                .videoUrl(BASE_URL + "/videos/apartment_tour_7.mp4")
                .amenities(Arrays.asList(
                        "Aromatic Garden",
                        "Sculpture Garden",
                        "Extensive Green Area",
                        "Amphitheatre",
                        "Jogging / Walking Track",
                        "Pergolas & Trellis Seating",
                        "Gazebo",
                        "Stone Seaters",
                        "Open Play Field",
                        "Informal Turf Area",
                        "Senior Citizen Park",
                        "Children’s Play Area / Tot Lot",
                        "Dense Buffer Plantation",
                        "Check Dam & Viewing Deck",
                        "Sand Pit",
                        "Off-Leash Dog Park",
                        "Clubhouse",
                        "Swimming Pool",
                        "Gym / Aerobics",
                        "Lounge",
                        "Billiards & Cards",
                        "Table Tennis",
                        "Carom & Chess",
                        "Library",
                        "Multipurpose Hall",
                        "Yoga & Meditation Center"
                ))
                .build();


// ------------------------- PROPERTY 8 -------------------------

        Property p8 = Property.builder()
                .title("Opening Soon – Villa Project")
                .slug(slugify("Villa-Project"))
                .location("Brigade Orchards, Devanahalli, Bengaluru")
                .price("₹95 Lakhs")
                .type("Plot")
                .sqft("2400")
                .reraApproved(true)
                .image(BASE_URL + "/images/p8/property-08-main-01.jpeg")
                .mainImages(Arrays.asList(
                        BASE_URL + "/images/p8/property-08-main-01.jpeg",
                        BASE_URL + "/images/p8/property-08-main-01.jpeg"
                ))
                .images(Arrays.asList(
                        BASE_URL + "/images/p8/property-08-gallery-01.jpeg",
                        BASE_URL + "/images/p8/property-08-gallery-02.jpeg",
                        BASE_URL + "/images/p8/property-08-gallery-03.jpeg",
                        BASE_URL + "/images/p8/property-08-gallery-04.jpeg",
                        BASE_URL + "/images/p8/property-08-gallery-05.jpeg"
                ))
                .bedrooms("0")
                .bathrooms("0")
                .carpetArea("N/A")
                .builtupArea("N/A")
                .parking("N/A")
                .maintenance("₹1 / Sqft")
                .furnishing("N/A")
                .facing("Any")
                .description("Premium 40x60 villa plot in a RERA-approved integrated township with 100+ amenities.")
                .videoUrl(BASE_URL + "/videos/plot_tour.mp4")
                .amenities(Arrays.asList(
                        "3-Phase Power Backup",
                        "24x7 Water Supply",
                        "Gated Community Security",
                        "Covered Car Parking",
                        "Clubhouse & Gym",
                        "Swimming Pool",
                        "Landscaped Garden",
                        "Children Play Area",
                        "High-Speed Internet Ready",
                        "Solar Water Heater",
                        "Modular Kitchen",
                        "Vitrified Tile Flooring"
                ))
                .build();


        repo.saveAll(Arrays.asList(p1, p2, p3, p4, p5, p6, p7, p8));

        System.out.println("✔ Sample properties loaded successfully.");
    }
}

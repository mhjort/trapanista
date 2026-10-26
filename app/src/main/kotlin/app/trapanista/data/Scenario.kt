package app.trapanista.data

data class Scenario(
    val id: String,
    val emoji: String,
    val title: String,
    val titleIt: String,
    val description: String,
    /** Who the tutor plays, described for the model. */
    val partnerRole: String,
    /** What the learner is trying to achieve, described for the model. */
    val learnerGoal: String,
)

val scenarios = listOf(
    Scenario(
        id = "restaurant",
        emoji = "🍝",
        title = "Ordering at a restaurant",
        titleIt = "Al ristorante",
        description = "Get a table, order a starter, main course and drink, then ask for the bill.",
        partnerRole = "a friendly waiter (cameriere) at a family-run trattoria in Rome",
        learnerGoal = "get seated, order food and a drink, and ask for the bill",
    ),
    Scenario(
        id = "hotel",
        emoji = "🏨",
        title = "Booking a hotel room",
        titleIt = "Prenotare una camera",
        description = "Reserve a double room for three nights and ask about breakfast and check-in.",
        partnerRole = "the receptionist at a small hotel in Florence, answering the phone",
        learnerGoal = "book a room for specific dates, ask the price, and ask whether breakfast is included",
    ),
    Scenario(
        id = "cafe",
        emoji = "☕",
        title = "At the café",
        titleIt = "Al bar",
        description = "Order a coffee and a pastry at the counter, the Italian way.",
        partnerRole = "a quick, cheerful barista at a busy bar in Naples",
        learnerGoal = "order a coffee and a cornetto, and pay",
    ),
    Scenario(
        id = "train",
        emoji = "🚆",
        title = "Buying a train ticket",
        titleIt = "Alla stazione",
        description = "Buy a ticket to another city and find out the platform and departure time.",
        partnerRole = "a ticket clerk at Milano Centrale station",
        learnerGoal = "buy a ticket to Venice, choose one-way or return, and find out the departure time and platform",
    ),
    Scenario(
        id = "directions",
        emoji = "🗺️",
        title = "Asking for directions",
        titleIt = "Chiedere indicazioni",
        description = "You're lost. Ask a local how to get to the museum.",
        partnerRole = "a helpful local passer-by in Bologna",
        learnerGoal = "politely stop someone, ask how to get to the museum, and understand the directions",
    ),
    Scenario(
        id = "market",
        emoji = "🍅",
        title = "Shopping at the market",
        titleIt = "Al mercato",
        description = "Buy fruit and vegetables: quantities, prices and paying.",
        partnerRole = "a chatty fruit and vegetable seller at an outdoor market in Palermo",
        learnerGoal = "buy a few items in specific quantities, ask prices, and pay",
    ),
    Scenario(
        id = "pharmacy",
        emoji = "💊",
        title = "At the pharmacy",
        titleIt = "In farmacia",
        description = "Describe a headache and a sore throat and get something for it.",
        partnerRole = "a calm, professional pharmacist (farmacista)",
        learnerGoal = "describe simple symptoms and get advice and medicine",
    ),
    Scenario(
        id = "smalltalk",
        emoji = "👋",
        title = "Meeting someone new",
        titleIt = "Fare conoscenza",
        description = "Introduce yourself to a new neighbour: name, where you're from, what you do.",
        partnerRole = "a friendly new neighbour in Trapani who is curious about the learner",
        learnerGoal = "introduce themselves, say where they are from and what they do, and ask a question back",
    ),
)

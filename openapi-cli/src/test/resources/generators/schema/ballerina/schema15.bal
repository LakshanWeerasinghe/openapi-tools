import ballerina/constraint;

@constraint:String {maxLength: 5000}
public type TaxratesItemsString string;

public type Activities record {
    # Position in pagination.
    int offset?;
    Activity[] history?;
};

public type User record {
    # First name of the Uber user.
    string first_name?;
    # Last name of the Uber user.
    string last_name?;
    TaxratesItemsString[]|"" tax_rates?;
};

public type AnyOF User|Activity;

public type Activity record {
    # Unique identifier for the activity
    string uuid?;
};

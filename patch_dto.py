import re

def process_file(filepath, is_request=False):
    with open(filepath, 'r') as f:
        content = f.read()

    if is_request:
        # Remove slug
        content = re.sub(r'\s*@NotBlank\(message = "Slug is required"\)\s*@Pattern\(regexp = "\^\[a-z0-9-\]\+\$", message = "Slug can only contain lowercase letters, numbers, and hyphens"\)\s*private String slug;\n', '\n', content)
        content = re.sub(r'\s*public String getSlug\(\) \{ return slug; \}\n', '\n', content)
        content = re.sub(r'\s*public void setSlug\(String slug\) \{ this.slug = slug; \}\n', '\n', content)

        # Remove @NotNull
        content = re.sub(r'\s*@NotNull\(message = ".*?"\)\n', '\n', content)
        
        # Remove @NotBlank except on name
        content = re.sub(r'(\s*)@NotBlank\(message = "Duration unit is required"\)\n', r'\1', content)

    # Add new fields
    new_fields = """
    private String caloriesLabel;
    private String deliveryInformation;
    private String terms;
    private String seoTitle;
    private String seoDescription;
"""
    content = content.replace('private JsonNode nutrition;', 'private JsonNode nutrition;' + new_fields)

    new_getters_setters = """
    public String getCaloriesLabel() { return caloriesLabel; }
    public void setCaloriesLabel(String caloriesLabel) { this.caloriesLabel = caloriesLabel; }
    public String getDeliveryInformation() { return deliveryInformation; }
    public void setDeliveryInformation(String deliveryInformation) { this.deliveryInformation = deliveryInformation; }
    public String getTerms() { return terms; }
    public void setTerms(String terms) { this.terms = terms; }
    public String getSeoTitle() { return seoTitle; }
    public void setSeoTitle(String seoTitle) { this.seoTitle = seoTitle; }
    public String getSeoDescription() { return seoDescription; }
    public void setSeoDescription(String seoDescription) { this.seoDescription = seoDescription; }
"""
    content = content.replace('public String getStatus()', new_getters_setters + '    public String getStatus()')

    with open(filepath, 'w') as f:
        f.write(content)

process_file('src/main/java/com/ofood/catalog/dto/PlanRequest.java', is_request=True)
process_file('src/main/java/com/ofood/catalog/dto/PlanResponse.java', is_request=False)

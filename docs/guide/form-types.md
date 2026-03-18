# Form Types

GeyserMenu supports three form types, each suitable for different scenarios.

## Form Types Overview

| Type | Description | Use Case |
|------|-------------|----------|
| SimpleForm | Simple form with multiple buttons | Main menu, navigation menu |
| ModalForm | Modal form with two buttons | Confirm operations, Yes/No selection |
| CustomForm | Custom form with multiple input components | Settings interface, user input collection |

## SimpleForm

SimpleForm is the most commonly used form type, containing a title, content, and multiple buttons with icons.

### Configuration Example

```yaml
menu:
  type: simple  # Can be omitted, default is simple
  title: "Main Menu"
  subtitle: "Select an option"
  content: "This is the menu content"
  footer: "Online players: %server_online%"
  
  items:
    - text: "Teleport Menu"
      description: "Open teleport menu"
      icon: "compass"
      icon_type: "java"
      submenu: "teleport.yml"
    
    - text: "Shop Menu"
      description: "Open shop menu"
      icon: "textures/items/diamond"
      icon_type: "bedrock"
      command: "shop"
```

### Configuration Options

| Option | Type | Required | Description |
|--------|------|----------|-------------|
| `type` | string | No | Form type, default `simple` |
| `title` | string | Yes | Form title |
| `subtitle` | string | No | Subtitle |
| `content` | string | No | Main content |
| `footer` | string | No | Footer |
| `items` | list | Yes | Button list |

### Button Options

| Option | Type | Required | Description |
|--------|------|----------|-------------|
| `text` | string | Yes | Button text |
| `description` | string | No | Button description |
| `icon` | string | Yes | Icon ID or path |
| `icon_type` | string | Yes | Icon type (java/bedrock/url) |
| `command` | string | No* | Command to execute |
| `submenu` | string | No* | Submenu file name |
| `execute_as` | string | No | Execution mode (player/console/op) |

*Either `command` or `submenu` is required.

## ModalForm

ModalForm is a confirmation dialog, suitable for operations requiring user confirmation.

### Configuration Example

```yaml
menu:
  type: modal
  title: "Confirm Purchase"
  content: |-
    Are you sure you want to spend 100 coins to buy a diamond?
    This action cannot be undone!
  button1: "Confirm Purchase"
  button2: "Cancel"
  
  on_button1:
    command: "eco take {player} 100 && give {player} diamond 1"
    execute_as: console
  
  on_button2:
    submenu: "shop.yml"
```

### Configuration Options

| Option | Type | Required | Description |
|--------|------|----------|-------------|
| `type` | string | Yes | Must be `modal` |
| `title` | string | Yes | Form title |
| `content` | string | Yes | Form content |
| `button1` | string | Yes | First button text |
| `button2` | string | Yes | Second button text |
| `on_button1` | section | No | Action for button 1 |
| `on_button2` | section | No | Action for button 2 |

### Use Cases

- Purchase confirmation
- Important operation confirmation
- Simple Yes/No selection

## CustomForm

CustomForm is the most flexible form type, supporting multiple input components, suitable for scenarios requiring user input.

### Configuration Example

```yaml
menu:
  type: custom
  title: "Player Settings"
  
  components:
    - type: label
      text: "=== Player Settings Panel ==="
    
    - type: dropdown
      text: "Select Language"
      options:
        - "Simplified Chinese"
        - "English"
        - "Japanese"
      default: 0
    
    - type: toggle
      text: "Receive Private Messages"
      default: true
    
    - type: slider
      text: "Render Distance"
      min: 2
      max: 32
      step: 2
      default: 12
    
    - type: input
      text: "Custom Nickname"
      placeholder: "Enter your nickname"
      default: "{player}"
  
  on_submit:
    command: "settings set {player} lang={0} pm={1} distance={2} name={3}"
    execute_as: console
```

### Component Types

#### Label

Displays static text, no input collection.

```yaml
- type: label
  text: "This is a label"
```

#### Input

Text input field.

```yaml
- type: input
  text: "Enter Name"
  placeholder: "Enter text here"
  default: ""
```

| Option | Description |
|--------|-------------|
| `text` | Input field label |
| `placeholder` | Placeholder text |
| `default` | Default value |

#### Dropdown

Dropdown selection.

```yaml
- type: dropdown
  text: "Select Option"
  options:
    - "Option 1"
    - "Option 2"
    - "Option 3"
  default: 0
```

| Option | Description |
|--------|-------------|
| `text` | Dropdown label |
| `options` | Options list |
| `default` | Default selected index (starting from 0) |

#### Slider

Numeric slider.

```yaml
- type: slider
  text: "Quantity"
  min: 1
  max: 64
  step: 1
  default: 1
```

| Option | Description |
|--------|-------------|
| `text` | Slider label |
| `min` | Minimum value |
| `max` | Maximum value |
| `step` | Step value |
| `default` | Default value |

#### Toggle

Boolean switch.

```yaml
- type: toggle
  text: "Enable Feature"
  default: false
```

| Option | Description |
|--------|-------------|
| `text` | Toggle label |
| `default` | Default state |

### Referencing Component Values

In `on_submit`, use `{0}`, `{1}`, `{2}...` to reference component values:

```yaml
on_submit:
  command: "say {0} selected {1}"
```

- `{0}` - First component value
- `{1}` - Second component value
- And so on...

:::tip Note
Label components are not indexed, only input-type components are indexed.
:::

## Best Practices

1. **Choose the Right Form Type**
   - Simple navigation: Use SimpleForm
   - Confirm operations: Use ModalForm
   - Collect input: Use CustomForm

2. **Combine Usage**
   - You can open ModalForm from SimpleForm buttons for confirmation
   - After CustomForm submission, you can open other menus

3. **Error Handling**
   - Always provide handling for both buttons in ModalForm
   - Validate user input in CustomForm

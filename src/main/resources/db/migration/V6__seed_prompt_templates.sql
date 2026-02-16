INSERT INTO prompt_templates (id, name, description, template_text, category) VALUES
    ('a1b2c3d4-e5f6-7890-abcd-ef1234567801',
     'Guitar Solo Generator',
     'Generates a guitar solo idea based on key, scale, and style',
     'Create a guitar solo in the key of {key} using the {scale} scale. Style: {style}. Tempo: {tempo} BPM. The solo should be {duration} bars long.',
     'SOLO'),

    ('a1b2c3d4-e5f6-7890-abcd-ef1234567802',
     'Chord Progression Suggester',
     'Suggests chord progressions for a given genre and mood',
     'Suggest a chord progression for a {genre} song with a {mood} mood. Key: {key}. Time signature: {timeSignature}. Length: {bars} bars.',
     'COMPOSITION'),

    ('a1b2c3d4-e5f6-7890-abcd-ef1234567803',
     'Practice Routine Builder',
     'Creates a structured practice routine for a given skill level',
     'Create a {duration}-minute guitar practice routine for a {level} player focusing on {focus}. Include warm-up, main exercises, and cool-down.',
     'PRACTICE'),

    ('a1b2c3d4-e5f6-7890-abcd-ef1234567804',
     'Riff Generator',
     'Generates a guitar riff idea based on genre and tuning',
     'Generate a guitar riff in {tuning} tuning for a {genre} song. Tempo: {tempo} BPM. Feel: {feel}. Use {technique} technique.',
     'RIFF'),

    ('a1b2c3d4-e5f6-7890-abcd-ef1234567805',
     'Backing Track Arrangement',
     'Suggests arrangement ideas for a backing track',
     'Suggest an arrangement for a backing track in {key} {scale}. Genre: {genre}. Tempo: {tempo} BPM. Instruments: {instruments}. Structure: {structure}.',
     'ARRANGEMENT');

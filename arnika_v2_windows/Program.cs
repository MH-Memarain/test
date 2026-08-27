using System.Diagnostics;
using System.Net.Http;
using System.Speech.Synthesis;
using System.Text.Json;
using NAudio.Wave;

namespace ArnikaSession;

internal static class Program
{
    [STAThread]
    static void Main(string[] args)
    {
        if (args.Contains("--self-test"))
        {
            var baseDir = AppContext.BaseDirectory;
            var ok = File.Exists(Path.Combine(baseDir,"tools","whisper-cli.exe"))
                     && File.Exists(Path.Combine(baseDir,"tools","ffmpeg.exe"))
                     && File.Exists(Path.Combine(baseDir,"models","ggml-tiny.bin"));
            Environment.Exit(ok ? 0 : 2);
        }
        ApplicationConfiguration.Initialize();
        Application.Run(new MainForm());
    }
}

public sealed class MainForm : Form
{
    readonly TextBox txtTitle = new(){PlaceholderText="عنوان جلسه"};
    readonly TextBox txtBody = new(){Multiline=true,ScrollBars=ScrollBars.Vertical,Font=new Font("Segoe UI",12),Dock=DockStyle.Fill};
    readonly Label lblFile = new(){Text="فایل انتخاب نشده",AutoSize=true};
    readonly Label lblStatus = new(){Text="آماده",AutoSize=true,ForeColor=Color.Teal};
    readonly ComboBox cmbTarget = new(){DropDownStyle=ComboBoxStyle.DropDownList};
    string? audioFile;
    string saveFolder = Environment.GetFolderPath(Environment.SpecialFolder.MyDocuments);
    WaveInEvent? waveIn; WaveFileWriter? waveWriter; string? recordedFile;
    readonly string baseDir = AppContext.BaseDirectory;

    public MainForm()
    {
        Text="ARNIKA SESSION 2.0"; Width=1050; Height=760; MinimumSize=new Size(900,650); StartPosition=FormStartPosition.CenterScreen;
        BackColor=Color.FromArgb(244,247,247); Font=new Font("Segoe UI",10);
        var root=new TableLayoutPanel{Dock=DockStyle.Fill,ColumnCount=2,RowCount=2,Padding=new Padding(18)};
        root.ColumnStyles.Add(new ColumnStyle(SizeType.Percent,72)); root.ColumnStyles.Add(new ColumnStyle(SizeType.Percent,28));
        root.RowStyles.Add(new RowStyle(SizeType.Absolute,72)); root.RowStyles.Add(new RowStyle(SizeType.Percent,100)); Controls.Add(root);

        var header=new Panel{Dock=DockStyle.Fill};
        header.Controls.Add(new Label{Text="ARNIKA SESSION",Font=new Font("Segoe UI Semibold",22),ForeColor=Color.FromArgb(8,47,44),AutoSize=true,Location=new Point(6,5)});
        header.Controls.Add(new Label{Text="Local multilingual transcription • Windows 10/11",ForeColor=Color.DimGray,AutoSize=true,Location=new Point(8,43)});
        root.Controls.Add(header,0,0); root.SetColumnSpan(header,2);

        var main=Card(); main.Dock=DockStyle.Fill; root.Controls.Add(main,0,1);
        var mainLayout=new TableLayoutPanel{Dock=DockStyle.Fill,RowCount=5,Padding=new Padding(15)};
        mainLayout.RowStyles.Add(new RowStyle(SizeType.Absolute,42)); mainLayout.RowStyles.Add(new RowStyle(SizeType.Percent,100)); mainLayout.RowStyles.Add(new RowStyle(SizeType.Absolute,42)); mainLayout.RowStyles.Add(new RowStyle(SizeType.Absolute,56)); mainLayout.RowStyles.Add(new RowStyle(SizeType.Absolute,34)); main.Controls.Add(mainLayout);
        txtTitle.Dock=DockStyle.Fill; mainLayout.Controls.Add(txtTitle,0,0);
        mainLayout.Controls.Add(txtBody,0,1);
        mainLayout.Controls.Add(lblFile,0,2);
        var actions=new FlowLayoutPanel{Dock=DockStyle.Fill,FlowDirection=FlowDirection.LeftToRight,WrapContents=false};
        actions.Controls.Add(Btn("انتخاب فایل صوتی",(_,_)=>PickAudio()));
        actions.Controls.Add(Btn("تبدیل فایل به متن",async(_,_)=>await TranscribeSelected(),true));
        actions.Controls.Add(Btn("ضبط ۸ ثانیه",(_,_)=>StartRecording()));
        actions.Controls.Add(Btn("توقف و تبدیل",async(_,_)=>await StopAndTranscribe()));
        mainLayout.Controls.Add(actions,0,3); mainLayout.Controls.Add(lblStatus,0,4);

        var side=Card(); side.Dock=DockStyle.Fill; root.Controls.Add(side,1,1);
        var tools=new FlowLayoutPanel{Dock=DockStyle.Fill,FlowDirection=FlowDirection.TopDown,WrapContents=false,Padding=new Padding(14),AutoScroll=true}; side.Controls.Add(tools);
        tools.Controls.Add(new Label{Text="ابزارها",Font=new Font("Segoe UI Semibold",16),ForeColor=Color.FromArgb(8,47,44),AutoSize=true,Margin=new Padding(3,3,3,12)});
        tools.Controls.Add(Btn("ذخیره TXT",(_,_)=>SaveTxt()));
        tools.Controls.Add(Btn("کپی متن",(_,_)=>{ if(!string.IsNullOrWhiteSpace(txtBody.Text)) Clipboard.SetText(txtBody.Text); SetStatus("متن کپی شد"); }));
        tools.Controls.Add(Btn("انتخاب پوشه خروجی",(_,_)=>ChooseFolder()));
        cmbTarget.Items.AddRange(new object[]{"fa فارسی","en English","ar العربية","fr Français","it Italiano","es Español","de Deutsch","tr Türkçe","ru Русский","zh 中文","ja 日本語","ko 한국어"}); cmbTarget.SelectedIndex=0; cmbTarget.Width=220; tools.Controls.Add(cmbTarget);
        tools.Controls.Add(Btn("ترجمه متن",async(_,_)=>await TranslateText()));
        tools.Controls.Add(Btn("تولید WAV از متن",(_,_)=>SpeakToWav()));
        tools.Controls.Add(Btn("تبدیل WAV به MP3",async(_,_)=>await ConvertLastWavToMp3()));
        tools.Controls.Add(Btn("پوسته روشن/تیره",(_,_)=>ToggleTheme()));
        tools.Controls.Add(new Label{Text="فایل‌های رایج مثل MP3/M4A/OGG/FLAC ابتدا با FFmpeg به WAV تبدیل و سپس با Whisper محلی پردازش می‌شوند.",MaximumSize=new Size(220,0),AutoSize=true,ForeColor=Color.DimGray,Margin=new Padding(3,14,3,3)});
    }

    Panel Card()=>new(){BackColor=Color.White,Padding=new Padding(2),Margin=new Padding(8)};
    Button Btn(string text,EventHandler click,bool primary=false){var b=new Button{Text=text,AutoSize=true,Height=38,FlatStyle=FlatStyle.Flat,Margin=new Padding(4)}; b.FlatAppearance.BorderSize=primary?0:1; if(primary){b.BackColor=Color.FromArgb(15,118,110);b.ForeColor=Color.White;} b.Click+=click; return b;}
    void SetStatus(string s){if(InvokeRequired){BeginInvoke(()=>SetStatus(s));return;} lblStatus.Text=s;}

    void PickAudio(){using var d=new OpenFileDialog{Filter="Audio/Video|*.wav;*.mp3;*.flac;*.m4a;*.aac;*.ogg;*.opus;*.mp4;*.mkv;*.webm|All files|*.*"}; if(d.ShowDialog()==DialogResult.OK){audioFile=d.FileName; lblFile.Text="فایل: "+Path.GetFileName(audioFile); SetStatus("فایل آمادهٔ تبدیل است");}}

    async Task TranscribeSelected(){if(string.IsNullOrWhiteSpace(audioFile)||!File.Exists(audioFile)){MessageBox.Show("ابتدا فایل صوتی را انتخاب کنید.");return;} await Transcribe(audioFile,false);}

    async Task Transcribe(string input,bool append)
    {
        try{
            SetStatus("در حال آماده‌سازی صدا…");
            var temp=Path.Combine(Path.GetTempPath(),"arnika_"+Guid.NewGuid()+".wav");
            await Run(Path.Combine(baseDir,"tools","ffmpeg.exe"),$"-y -i \"{input}\" -ar 16000 -ac 1 -c:a pcm_s16le \"{temp}\"");
            SetStatus("Whisper در حال تبدیل صدا به متن…");
            var outBase=Path.Combine(Path.GetTempPath(),"arnika_out_"+Guid.NewGuid());
            await Run(Path.Combine(baseDir,"tools","whisper-cli.exe"),$"-m \"{Path.Combine(baseDir,"models","ggml-tiny.bin")}\" -f \"{temp}\" -l auto -otxt -of \"{outBase}\" -nt");
            var outTxt=outBase+".txt"; if(!File.Exists(outTxt)) throw new Exception("Whisper output file not found");
            var text=await File.ReadAllTextAsync(outTxt);
            if(append && txtBody.TextLength>0) txtBody.AppendText(Environment.NewLine+text.Trim()); else txtBody.Text=text.Trim();
            SetStatus("تبدیل با موفقیت انجام شد");
            TryDelete(temp); TryDelete(outTxt);
        }catch(Exception ex){SetStatus("خطا: "+ex.Message); MessageBox.Show(ex.Message,"ARNIKA SESSION",MessageBoxButtons.OK,MessageBoxIcon.Error);}
    }

    async Task<int> Run(string exe,string args)
    {
        var psi=new ProcessStartInfo(exe,args){UseShellExecute=false,CreateNoWindow=true,RedirectStandardError=true,RedirectStandardOutput=true};
        using var p=Process.Start(psi) ?? throw new Exception("اجرای ابزار ممکن نشد: "+exe);
        var so=p.StandardOutput.ReadToEndAsync(); var se=p.StandardError.ReadToEndAsync(); await p.WaitForExitAsync(); var err=await se; _=await so; if(p.ExitCode!=0) throw new Exception(err.Length>700?err[^700..]:err); return p.ExitCode;
    }

    void StartRecording()
    {
        try{
            if(waveIn!=null)return; recordedFile=Path.Combine(Path.GetTempPath(),"arnika_mic_"+Guid.NewGuid()+".wav");
            waveIn=new WaveInEvent{WaveFormat=new WaveFormat(16000,1),BufferMilliseconds=100}; waveWriter=new WaveFileWriter(recordedFile,waveIn.WaveFormat);
            waveIn.DataAvailable+=(s,e)=>waveWriter?.Write(e.Buffer,0,e.BytesRecorded); waveIn.RecordingStopped+=(s,e)=>{waveWriter?.Dispose();waveWriter=null;waveIn?.Dispose();waveIn=null;}; waveIn.StartRecording(); SetStatus("در حال ضبط میکروفن…");
        }catch(Exception ex){MessageBox.Show(ex.Message);}
    }
    async Task StopAndTranscribe(){if(waveIn==null||recordedFile==null){MessageBox.Show("ضبطی در حال انجام نیست.");return;} waveIn.StopRecording(); await Task.Delay(350); await Transcribe(recordedFile,true); TryDelete(recordedFile); recordedFile=null;}

    void ChooseFolder(){using var f=new FolderBrowserDialog{SelectedPath=saveFolder}; if(f.ShowDialog()==DialogResult.OK){saveFolder=f.SelectedPath;SetStatus("پوشه خروجی تغییر کرد");}}
    void SaveTxt(){Directory.CreateDirectory(saveFolder);var name=Safe(string.IsNullOrWhiteSpace(txtTitle.Text)?"ARNIKA_SESSION":txtTitle.Text)+"_"+DateTime.Now.ToString("yyyyMMdd_HHmmss")+".txt";File.WriteAllText(Path.Combine(saveFolder,name),txtBody.Text);SetStatus("TXT ذخیره شد");}

    async Task TranslateText()
    {
        if(string.IsNullOrWhiteSpace(txtBody.Text))return; try{SetStatus("در حال ترجمه…");var code=cmbTarget.SelectedItem!.ToString()!.Split(' ')[0];using var http=new HttpClient();var url="https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl="+code+"&dt=t&q="+Uri.EscapeDataString(txtBody.Text);var json=await http.GetStringAsync(url);using var doc=JsonDocument.Parse(json);var sb=new System.Text.StringBuilder();foreach(var seg in doc.RootElement[0].EnumerateArray()) if(seg.ValueKind==JsonValueKind.Array && seg.GetArrayLength()>0) sb.Append(seg[0].GetString());txtBody.Text=sb.ToString();SetStatus("ترجمه انجام شد");}catch(Exception ex){SetStatus("ترجمه ناموفق: "+ex.Message);}
    }

    string? lastWav;
    void SpeakToWav(){try{if(string.IsNullOrWhiteSpace(txtBody.Text))return;Directory.CreateDirectory(saveFolder);lastWav=Path.Combine(saveFolder,Safe(string.IsNullOrWhiteSpace(txtTitle.Text)?"ARNIKA_TTS":txtTitle.Text)+"_tts.wav");using var s=new SpeechSynthesizer();s.SetOutputToWaveFile(lastWav);s.Speak(txtBody.Text);s.SetOutputToDefaultAudioDevice();SetStatus("WAV تولید شد");}catch(Exception ex){MessageBox.Show(ex.Message);}}
    async Task ConvertLastWavToMp3(){if(lastWav==null||!File.Exists(lastWav)){MessageBox.Show("ابتدا فایل WAV تولید کنید.");return;}var mp3=Path.ChangeExtension(lastWav,".mp3");try{await Run(Path.Combine(baseDir,"tools","ffmpeg.exe"),$"-y -i \"{lastWav}\" \"{mp3}\"");SetStatus("MP3 تولید شد");}catch(Exception ex){MessageBox.Show(ex.Message);}}

    bool dark=false;void ToggleTheme(){dark=!dark;BackColor=dark?Color.FromArgb(28,32,34):Color.FromArgb(244,247,247);txtBody.BackColor=dark?Color.FromArgb(40,45,47):Color.White;txtBody.ForeColor=dark?Color.Gainsboro:Color.Black;SetStatus(dark?"پوسته تیره":"پوسته روشن");}
    static string Safe(string s)=>string.Concat(s.Select(c=>Path.GetInvalidFileNameChars().Contains(c)?'_':c));
    static void TryDelete(string? p){try{if(p!=null&&File.Exists(p))File.Delete(p);}catch{}}
}
